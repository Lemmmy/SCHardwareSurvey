require("dotenv").config();

const _ = require("lodash");
const path = require("path");

const express = require("express");
const exphbs = require("express-handlebars");
const bodyParser = require("body-parser");
const { MongoClient } = require("mongodb");

const zen = require("zen-quote");

const STATS = require("./stats.json");

const UUID = /^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$/;
const UA_MATCH = new RegExp(`^SCHWS/${process.env.MC_VERSION}/${process.env.MOD_VERSION}$`);
const GL_VERSION = /^(\d+\.\d+)/;

let collection;

const app = express();

app.use(bodyParser.json());

app.engine(".hbs", exphbs({
  extname: ".hbs",
  helpers: {
    /** Returns a formatted percentage */
    percentage(n, m) {
      return ((n / m) * 100).toFixed(1);
    }
  }
}));
app.set("view engine", ".hbs");

function validateUserAgent(req) {
  const ua = req.get("User-Agent");
  return ua && UA_MATCH.test(ua);
}

app.post("/submit/:token", async (req, res) => {
  if (!req.params.token || !UUID.test(req.params.token))
    return res.json({ ok: false, error: "invalid_token" });
  if (!req.body || !req.body.stats)
    return res.json({ ok: false, error: "missing_stats" });
  if (!validateUserAgent(req))
    return res.json({ ok: false, error: "invalid_client" });

  const { stats } = req.body;

  // validate the stats
  for (let key in stats) {
    if (!key.startsWith("jvm_arg[") && (!STATS.includes(key) || typeof(stats[key]) !== "string"))
      return res.json({ ok: false, error: "invalid_stat", stat: key });

    stats[key] = stats[key].substring(0, Math.min(stats[key].length, 512));
  }

  // jvm_args is an array, so let's handle that
  let jvmArgs = [];
  if (stats["jvm_args"]) {
    const jvmArgsCount = parseInt(stats["jvm_args"]);
    if (isNaN(jvmArgsCount)) return res.json({ ok: false, error: "invalid_jvm_args" });

    for (let i = 0; i < jvmArgsCount; i++) {
      const jvmArgKey = `jvm_arg[${i}]`;
      if (!stats[jvmArgKey] || typeof(stats[jvmArgKey]) !== "string")
        return res.json({ ok: false, error: "invalid_jvm_args" });

      jvmArgs[i] = stats[jvmArgKey];
      delete stats[jvmArgKey];
      stats.jvm_args = jvmArgs;
    }
  }

  // insert to the DB
  try {
    await collection.insertOne({
      stats,
      createdAt: new Date(),
      token: req.params.token
    });
  } catch (err) {
    if (err.name === "MongoError" && err.code === 11000)
      return res.json({ ok: false, error: "already_submitted" });
    else
      return res.json({ ok: false, error: "unknown_error" });
  }

  res.json({ ok: true, upliftHeadThought: zen() });
});

/** Simplifies an OpenGL version string down to a major.minor version. */
function simplifyOpenGLVersion(version) {
  const match = GL_VERSION.exec(version.trim());
  if (!match) return "unknown";
  return match[1];
}

/** Compares two OpenGL versions. Returns true if `b` >= `a`. */
function compareOpenGLVersions(a, b) {
  return b.localeCompare(a, undefined, { numeric: true, sensitivity: "base" }) >= 0
}

/** Check if a result has the specified OpenGL capability. */
function hasOpenGLCapability(result, cap) {
  return result[`gl_caps[${cap}]`] === "true";
}

/** Returns an array of tuples by [value, users, percentage]. */
function getGrouped(results, key, groupBy) {
  return _(results)
    .map(key)
    .groupBy(groupBy)
    .mapValues("length")
    .toPairs()
    .sortBy(v => v[0])
    .map(v => ({ value: v[0], count: v[1] }))
    .value();
}

/** Returns an array of tuples by [version, users, percentage]. */
function getOpenGLVersions(results) {
  return getGrouped(results, "opengl_version", simplifyOpenGLVersion);
}

/** Returns an array of features and which users support them. */
function getOpenGLFeatures(results, openGLVersions) {
  return {
    features: [
      {
        value: "Texture Buffer Objects",
        link: "https://www.khronos.org/opengl/wiki/Buffer_Texture",
        count: _(results)
          .filter(r =>
            compareOpenGLVersions("3.1", simplifyOpenGLVersion(r.opengl_version)) ||
            hasOpenGLCapability(r, "ARB_texture_buffer_object") ||
            hasOpenGLCapability(r, "EXT_texture_buffer_object")
          )
          .size()
      },
      {
        value: "Uniform Buffer Objects",
        link: "https://www.khronos.org/opengl/wiki/Uniform_Buffer_Object",
        count: _(results)
          .filter(r =>
            compareOpenGLVersions("3.1", simplifyOpenGLVersion(r.opengl_version)) ||
            hasOpenGLCapability(r, "ARB_uniform_buffer_object")
          )
          .size()
      }
    ],
    individualFeatures: [
      {
        value: "OpenGL 3.1",
        count: _(openGLVersions)
          .filter(v => compareOpenGLVersions("3.1", v.value))
          .sumBy("count")
      },
      {
        value: "ARB_texture_buffer_object",
        link: "https://www.khronos.org/registry/OpenGL/extensions/ARB/ARB_texture_buffer_object.txt",
        count: _(results).filter(r => hasOpenGLCapability(r, "ARB_texture_buffer_object")).size()
      },
      {
        value: "EXT_texture_buffer_object",
        link: "https://www.khronos.org/registry/OpenGL/extensions/EXT/EXT_texture_buffer_object.txt",
        count: _(results).filter(r => hasOpenGLCapability(r, "EXT_texture_buffer_object")).size()
      },
      {
        value: "ARB_uniform_buffer_object",
        link: "http://www.opengl.org/registry/specs/ARB/uniform_buffer_object.txt",
        count: _(results).filter(r => hasOpenGLCapability(r, "ARB_uniform_buffer_object")).size()
      }
    ],
    maxTextureSize: _(getGrouped(results, "gl_max_texture_size"))
      .mapValues((value, key) => key === "value" ? parseInt(value) : value)
      .sortBy("value")
      .value()
  }
}

/** Returns an array of tuples by [os, users, percentage]. */
function getOSList(results) {
  return getGrouped(results, "os_name");
}

/** Returns an array of tuples by [arch, users, percentage]. */
function getOSArches(results) {
  const arches = getGrouped(results, "os_architecture", a => a.replace(/^x86_64$/, "amd64"));
  arches.forEach(arch => {
    if (arch.value === "aarch64") {
      arch.link = "https://i.lemmmy.pw/oE9n.jpg";
    }
  });
  return arches;
}

/** Returns an array of tuples by [mod, users, percentage]. */
function getModList(results) {
  return [
    {
      value: "OptiFine",
      count: _(results).filter(r => !!r.optifine_version).size()
    },
    {
      value: "FoamFix",
      count: _(results).filter(r => !!r.foamfix_version).size()
    },
    {
      value: "MultiMC",
      count: _(results)
        .filter(r => r.launched_version && /^MultiMC/.test(r.launched_version))
        .size()
    }
  ]
}

app.use(async (req, res) => {
  // We can process the results in the DB, but considering they're all going to be used at once anyway, we may as well
  // process them here.
  const results = _.map(await collection.find({}).toArray(), "stats");
  const count = results.length;
  const openGLVersions = getOpenGLVersions(results);

  res.render("home", {
    count,
    openGLVersions,
    features: getOpenGLFeatures(results, openGLVersions),
    osList: getOSList(results),
    osArches: getOSArches(results),
    modList: getModList(results),
  });
});

MongoClient.connect("mongodb://localhost:27017/schws", {
  useUnifiedTopology: true,
  useNewUrlParser: true,
}).then(client => {
  collection = client.db().collection("surveys");
  collection.createIndex({ "token": 1 }, { unique: true });

  app.listen(46444, () => console.log(`Ready, listening on port 46444`));
}).catch(console.error);