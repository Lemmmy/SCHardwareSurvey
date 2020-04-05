require("dotenv").config();

const path = require("path");

const express = require("express");
const exphbs = require("express-handlebars");
const bodyParser = require("body-parser");
const { MongoClient } = require("mongodb");

const zen = require("zen-quote");

const STATS = require("./stats.json");

const UUID = /^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$/;
const UA_MATCH = new RegExp(`^SCHWS/${process.env.MC_VERSION}/${process.env.MOD_VERSION}$`);

let collection;

const app = express();

app.use(bodyParser.json());

app.engine(".hbs", exphbs({
  extname: ".hbs"
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

app.use((req, res) => {
  res.render("home");
});

MongoClient.connect("mongodb://localhost:27017/schws", {
  useUnifiedTopology: true,
  useNewUrlParser: true,
}).then(client => {  
  collection = client.db().collection("surveys");
  collection.createIndex({ "token": 1 }, { unique: true });

  app.listen(46444, () => console.log(`Ready, listening on port 46444`));
}).catch(console.error);