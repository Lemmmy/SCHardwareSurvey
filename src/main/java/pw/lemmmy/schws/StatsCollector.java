package pw.lemmmy.schws;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.*;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.Processor;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

// Based on net.minecraft.profiler.Snooper
public class StatsCollector {
    private final Map<String, String> stats = new LinkedHashMap<>();
    
    private void addStat(String key, Object value) {
        stats.put(key, Objects.toString(value));
    }
    
    void collectStats() {
        collectJVMArgs();
        collectOSData();
        collectMemoryStats();
        collectHardwareData();
        collectMinecraftData();
        collectModData();
        collectOpenGLData();
        collectOpenGLCaps();
    }
    
    private void collectJVMArgs() {
        try {
            final RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
            final List<String> list = runtimemxbean.getInputArguments();
        
            int i = 0;
            for (String s : list) {
                if (s.startsWith("-X")) {
                    addStat(String.format("jvm_arg[%d]", i++), s);
                }
            }
        
            addStat("jvm_args", i);
        } catch (Throwable ignored) {}
    }
    
    private void collectOSData() {
        try {
            addStat("os_name", System.getProperty("os.name"));
            addStat("os_version", System.getProperty("os.version"));
            addStat("os_architecture", System.getProperty("os.arch"));
            addStat("java_version", System.getProperty("java.version"));
            addStat("java_64bit", Boolean.toString(isJvm64bit()));
        } catch (Throwable ignored) {}
    }
    
    private void collectMemoryStats() {
        try {
            addStat("memory_total", Runtime.getRuntime().totalMemory());
            addStat("memory_max", Runtime.getRuntime().maxMemory());
        } catch (Throwable ignored) {}
    }
    
    private void collectHardwareData() {
        try {
            addStat("cpu_cores", Runtime.getRuntime().availableProcessors());
            
            // based on OpenGlHelper.getCpu(), which is not available at runtime for some reason...
            HardwareAbstractionLayer hal = new SystemInfo().getHardware();
            
            Processor[] processor = hal.getProcessors();
            addStat("cpu_model", String.format("%dx %s", processor.length, processor[0]).replaceAll("\\s+", " "));
            
            Memory memory = hal.getMemory();
            addStat("memory_system_total", memory.getTotal());
        } catch (Throwable ignored) {}
        
        addStat("display_model", GlStateManager.glGetString(GL11.GL_RENDERER));
    }
    
    private void collectMinecraftData() {
        try {
            addStat("client_brand", ClientBrandRetriever.getClientModName());
            addStat("launched_version", Minecraft.getMinecraft().getVersion());
        } catch (Throwable ignored) {}
    }
    
    private void collectModData() {
        addStat("forge_version", ForgeVersion.getVersion());
        getOptiFineVersion().ifPresent(v -> addStat("optifine_version", v));
        getFoamFixVersion().ifPresent(v -> addStat("foamfix_version", v));
    }
    
    private void collectOpenGLData() {
        addStat("opengl_version", GlStateManager.glGetString(GL11.GL_VERSION));
        addStat("opengl_vendor", GlStateManager.glGetString(GL11.GL_VENDOR));
    
        ContextCapabilities caps = GLContext.getCapabilities();
        if (caps.GL_NVX_gpu_memory_info) { // get VRAM on NVIDIA cards
            addStat("opengl_memory", GlStateManager.glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX));
            GlStateManager.glGetError();
        }
    }
    
    private void collectOpenGLCaps() {
        addStat("gl_max_texture_size", SplashProgress.getMaxTextureSize());
        
        ContextCapabilities caps = GLContext.getCapabilities();
        addStat("gl_caps[ARB_ES2_compatibility]", caps.GL_ARB_ES2_compatibility);
        addStat("gl_caps[ARB_ES3_1_compatibility]", caps.GL_ARB_ES3_1_compatibility);
        addStat("gl_caps[ARB_ES3_compatibility]", caps.GL_ARB_ES3_compatibility);
        addStat("gl_caps[ARB_arrays_of_arrays]", caps.GL_ARB_arrays_of_arrays);
        addStat("gl_caps[ARB_base_instance]", caps.GL_ARB_base_instance);
        addStat("gl_caps[ARB_bindless_texture]", caps.GL_ARB_bindless_texture);
        addStat("gl_caps[ARB_blend_func_extended]", caps.GL_ARB_blend_func_extended);
        addStat("gl_caps[ARB_buffer_storage]", caps.GL_ARB_buffer_storage);
        addStat("gl_caps[ARB_cl_event]", caps.GL_ARB_cl_event);
        addStat("gl_caps[ARB_clear_buffer_object]", caps.GL_ARB_clear_buffer_object);
        addStat("gl_caps[ARB_clear_texture]", caps.GL_ARB_clear_texture);
        addStat("gl_caps[ARB_clip_control]", caps.GL_ARB_clip_control);
        addStat("gl_caps[ARB_color_buffer_float]", caps.GL_ARB_color_buffer_float);
        addStat("gl_caps[ARB_compatibility]", caps.GL_ARB_compatibility);
        addStat("gl_caps[ARB_compressed_texture_pixel_storage]", caps.GL_ARB_compressed_texture_pixel_storage);
        addStat("gl_caps[ARB_compute_shader]", caps.GL_ARB_compute_shader);
        addStat("gl_caps[ARB_compute_variable_group_size]", caps.GL_ARB_compute_variable_group_size);
        addStat("gl_caps[ARB_conditional_render_inverted]", caps.GL_ARB_conditional_render_inverted);
        addStat("gl_caps[ARB_conservative_depth]", caps.GL_ARB_conservative_depth);
        addStat("gl_caps[ARB_copy_buffer]", caps.GL_ARB_copy_buffer);
        addStat("gl_caps[ARB_copy_image]", caps.GL_ARB_copy_image);
        addStat("gl_caps[ARB_cull_distance]", caps.GL_ARB_cull_distance);
        addStat("gl_caps[ARB_debug_output]", caps.GL_ARB_debug_output);
        addStat("gl_caps[ARB_depth_buffer_float]", caps.GL_ARB_depth_buffer_float);
        addStat("gl_caps[ARB_depth_clamp]", caps.GL_ARB_depth_clamp);
        addStat("gl_caps[ARB_depth_texture]", caps.GL_ARB_depth_texture);
        addStat("gl_caps[ARB_derivative_control]", caps.GL_ARB_derivative_control);
        addStat("gl_caps[ARB_direct_state_access]", caps.GL_ARB_direct_state_access);
        addStat("gl_caps[ARB_draw_buffers]", caps.GL_ARB_draw_buffers);
        addStat("gl_caps[ARB_draw_buffers_blend]", caps.GL_ARB_draw_buffers_blend);
        addStat("gl_caps[ARB_draw_elements_base_vertex]", caps.GL_ARB_draw_elements_base_vertex);
        addStat("gl_caps[ARB_draw_indirect]", caps.GL_ARB_draw_indirect);
        addStat("gl_caps[ARB_draw_instanced]", caps.GL_ARB_draw_instanced);
        addStat("gl_caps[ARB_enhanced_layouts]", caps.GL_ARB_enhanced_layouts);
        addStat("gl_caps[ARB_explicit_attrib_location]", caps.GL_ARB_explicit_attrib_location);
        addStat("gl_caps[ARB_explicit_uniform_location]", caps.GL_ARB_explicit_uniform_location);
        addStat("gl_caps[ARB_fragment_coord_conventions]", caps.GL_ARB_fragment_coord_conventions);
        addStat("gl_caps[ARB_fragment_layer_viewport]", caps.GL_ARB_fragment_layer_viewport);
        addStat("gl_caps[ARB_fragment_program]", caps.GL_ARB_fragment_program);
        addStat("gl_caps[ARB_fragment_program_shadow]", caps.GL_ARB_fragment_program_shadow);
        addStat("gl_caps[ARB_fragment_shader]", caps.GL_ARB_fragment_shader);
        addStat("gl_caps[ARB_framebuffer_no_attachments]", caps.GL_ARB_framebuffer_no_attachments);
        addStat("gl_caps[ARB_framebuffer_object]", caps.GL_ARB_framebuffer_object);
        addStat("gl_caps[ARB_framebuffer_sRGB]", caps.GL_ARB_framebuffer_sRGB);
        addStat("gl_caps[ARB_geometry_shader4]", caps.GL_ARB_geometry_shader4);
        addStat("gl_caps[ARB_get_program_binary]", caps.GL_ARB_get_program_binary);
        addStat("gl_caps[ARB_get_texture_sub_image]", caps.GL_ARB_get_texture_sub_image);
        addStat("gl_caps[ARB_gpu_shader5]", caps.GL_ARB_gpu_shader5);
        addStat("gl_caps[ARB_gpu_shader_fp64]", caps.GL_ARB_gpu_shader_fp64);
        addStat("gl_caps[ARB_half_float_pixel]", caps.GL_ARB_half_float_pixel);
        addStat("gl_caps[ARB_half_float_vertex]", caps.GL_ARB_half_float_vertex);
        addStat("gl_caps[ARB_imaging]", caps.GL_ARB_imaging);
        addStat("gl_caps[ARB_indirect_parameters]", caps.GL_ARB_indirect_parameters);
        addStat("gl_caps[ARB_instanced_arrays]", caps.GL_ARB_instanced_arrays);
        addStat("gl_caps[ARB_internalformat_query]", caps.GL_ARB_internalformat_query);
        addStat("gl_caps[ARB_internalformat_query2]", caps.GL_ARB_internalformat_query2);
        addStat("gl_caps[ARB_invalidate_subdata]", caps.GL_ARB_invalidate_subdata);
        addStat("gl_caps[ARB_map_buffer_alignment]", caps.GL_ARB_map_buffer_alignment);
        addStat("gl_caps[ARB_map_buffer_range]", caps.GL_ARB_map_buffer_range);
        addStat("gl_caps[ARB_matrix_palette]", caps.GL_ARB_matrix_palette);
        addStat("gl_caps[ARB_multi_bind]", caps.GL_ARB_multi_bind);
        addStat("gl_caps[ARB_multi_draw_indirect]", caps.GL_ARB_multi_draw_indirect);
        addStat("gl_caps[ARB_multisample]", caps.GL_ARB_multisample);
        addStat("gl_caps[ARB_multitexture]", caps.GL_ARB_multitexture);
        addStat("gl_caps[ARB_occlusion_query]", caps.GL_ARB_occlusion_query);
        addStat("gl_caps[ARB_occlusion_query2]", caps.GL_ARB_occlusion_query2);
        addStat("gl_caps[ARB_pipeline_statistics_query]", caps.GL_ARB_pipeline_statistics_query);
        addStat("gl_caps[ARB_pixel_buffer_object]", caps.GL_ARB_pixel_buffer_object);
        addStat("gl_caps[ARB_point_parameters]", caps.GL_ARB_point_parameters);
        addStat("gl_caps[ARB_point_sprite]", caps.GL_ARB_point_sprite);
        addStat("gl_caps[ARB_program_interface_query]", caps.GL_ARB_program_interface_query);
        addStat("gl_caps[ARB_provoking_vertex]", caps.GL_ARB_provoking_vertex);
        addStat("gl_caps[ARB_query_buffer_object]", caps.GL_ARB_query_buffer_object);
        addStat("gl_caps[ARB_robust_buffer_access_behavior]", caps.GL_ARB_robust_buffer_access_behavior);
        addStat("gl_caps[ARB_robustness]", caps.GL_ARB_robustness);
        addStat("gl_caps[ARB_robustness_isolation]", caps.GL_ARB_robustness_isolation);
        addStat("gl_caps[ARB_sample_shading]", caps.GL_ARB_sample_shading);
        addStat("gl_caps[ARB_sampler_objects]", caps.GL_ARB_sampler_objects);
        addStat("gl_caps[ARB_seamless_cube_map]", caps.GL_ARB_seamless_cube_map);
        addStat("gl_caps[ARB_seamless_cubemap_per_texture]", caps.GL_ARB_seamless_cubemap_per_texture);
        addStat("gl_caps[ARB_separate_shader_objects]", caps.GL_ARB_separate_shader_objects);
        addStat("gl_caps[ARB_shader_atomic_counters]", caps.GL_ARB_shader_atomic_counters);
        addStat("gl_caps[ARB_shader_bit_encoding]", caps.GL_ARB_shader_bit_encoding);
        addStat("gl_caps[ARB_shader_draw_parameters]", caps.GL_ARB_shader_draw_parameters);
        addStat("gl_caps[ARB_shader_group_vote]", caps.GL_ARB_shader_group_vote);
        addStat("gl_caps[ARB_shader_image_load_store]", caps.GL_ARB_shader_image_load_store);
        addStat("gl_caps[ARB_shader_image_size]", caps.GL_ARB_shader_image_size);
        addStat("gl_caps[ARB_shader_objects]", caps.GL_ARB_shader_objects);
        addStat("gl_caps[ARB_shader_precision]", caps.GL_ARB_shader_precision);
        addStat("gl_caps[ARB_shader_stencil_export]", caps.GL_ARB_shader_stencil_export);
        addStat("gl_caps[ARB_shader_storage_buffer_object]", caps.GL_ARB_shader_storage_buffer_object);
        addStat("gl_caps[ARB_shader_subroutine]", caps.GL_ARB_shader_subroutine);
        addStat("gl_caps[ARB_shader_texture_image_samples]", caps.GL_ARB_shader_texture_image_samples);
        addStat("gl_caps[ARB_shader_texture_lod]", caps.GL_ARB_shader_texture_lod);
        addStat("gl_caps[ARB_shading_language_100]", caps.GL_ARB_shading_language_100);
        addStat("gl_caps[ARB_shading_language_420pack]", caps.GL_ARB_shading_language_420pack);
        addStat("gl_caps[ARB_shading_language_include]", caps.GL_ARB_shading_language_include);
        addStat("gl_caps[ARB_shading_language_packing]", caps.GL_ARB_shading_language_packing);
        addStat("gl_caps[ARB_shadow]", caps.GL_ARB_shadow);
        addStat("gl_caps[ARB_shadow_ambient]", caps.GL_ARB_shadow_ambient);
        addStat("gl_caps[ARB_sparse_buffer]", caps.GL_ARB_sparse_buffer);
        addStat("gl_caps[ARB_sparse_texture]", caps.GL_ARB_sparse_texture);
        addStat("gl_caps[ARB_stencil_texturing]", caps.GL_ARB_stencil_texturing);
        addStat("gl_caps[ARB_sync]", caps.GL_ARB_sync);
        addStat("gl_caps[ARB_tessellation_shader]", caps.GL_ARB_tessellation_shader);
        addStat("gl_caps[ARB_texture_barrier]", caps.GL_ARB_texture_barrier);
        addStat("gl_caps[ARB_texture_border_clamp]", caps.GL_ARB_texture_border_clamp);
        addStat("gl_caps[ARB_texture_buffer_object]", caps.GL_ARB_texture_buffer_object);
        addStat("gl_caps[ARB_texture_buffer_object_rgb32]", caps.GL_ARB_texture_buffer_object_rgb32);
        addStat("gl_caps[ARB_texture_buffer_range]", caps.GL_ARB_texture_buffer_range);
        addStat("gl_caps[ARB_texture_compression]", caps.GL_ARB_texture_compression);
        addStat("gl_caps[ARB_texture_compression_bptc]", caps.GL_ARB_texture_compression_bptc);
        addStat("gl_caps[ARB_texture_compression_rgtc]", caps.GL_ARB_texture_compression_rgtc);
        addStat("gl_caps[ARB_texture_cube_map]", caps.GL_ARB_texture_cube_map);
        addStat("gl_caps[ARB_texture_cube_map_array]", caps.GL_ARB_texture_cube_map_array);
        addStat("gl_caps[ARB_texture_env_add]", caps.GL_ARB_texture_env_add);
        addStat("gl_caps[ARB_texture_env_combine]", caps.GL_ARB_texture_env_combine);
        addStat("gl_caps[ARB_texture_env_crossbar]", caps.GL_ARB_texture_env_crossbar);
        addStat("gl_caps[ARB_texture_env_dot3]", caps.GL_ARB_texture_env_dot3);
        addStat("gl_caps[ARB_texture_float]", caps.GL_ARB_texture_float);
        addStat("gl_caps[ARB_texture_gather]", caps.GL_ARB_texture_gather);
        addStat("gl_caps[ARB_texture_mirror_clamp_to_edge]", caps.GL_ARB_texture_mirror_clamp_to_edge);
        addStat("gl_caps[ARB_texture_mirrored_repeat]", caps.GL_ARB_texture_mirrored_repeat);
        addStat("gl_caps[ARB_texture_multisample]", caps.GL_ARB_texture_multisample);
        addStat("gl_caps[ARB_texture_non_power_of_two]", caps.GL_ARB_texture_non_power_of_two);
        addStat("gl_caps[ARB_texture_query_levels]", caps.GL_ARB_texture_query_levels);
        addStat("gl_caps[ARB_texture_query_lod]", caps.GL_ARB_texture_query_lod);
        addStat("gl_caps[ARB_texture_rectangle]", caps.GL_ARB_texture_rectangle);
        addStat("gl_caps[ARB_texture_rg]", caps.GL_ARB_texture_rg);
        addStat("gl_caps[ARB_texture_rgb10_a2ui]", caps.GL_ARB_texture_rgb10_a2ui);
        addStat("gl_caps[ARB_texture_stencil8]", caps.GL_ARB_texture_stencil8);
        addStat("gl_caps[ARB_texture_storage]", caps.GL_ARB_texture_storage);
        addStat("gl_caps[ARB_texture_storage_multisample]", caps.GL_ARB_texture_storage_multisample);
        addStat("gl_caps[ARB_texture_swizzle]", caps.GL_ARB_texture_swizzle);
        addStat("gl_caps[ARB_texture_view]", caps.GL_ARB_texture_view);
        addStat("gl_caps[ARB_timer_query]", caps.GL_ARB_timer_query);
        addStat("gl_caps[ARB_transform_feedback2]", caps.GL_ARB_transform_feedback2);
        addStat("gl_caps[ARB_transform_feedback3]", caps.GL_ARB_transform_feedback3);
        addStat("gl_caps[ARB_transform_feedback_instanced]", caps.GL_ARB_transform_feedback_instanced);
        addStat("gl_caps[ARB_transform_feedback_overflow_query]", caps.GL_ARB_transform_feedback_overflow_query);
        addStat("gl_caps[ARB_transpose_matrix]", caps.GL_ARB_transpose_matrix);
        addStat("gl_caps[ARB_uniform_buffer_object]", caps.GL_ARB_uniform_buffer_object);
        addStat("gl_caps[ARB_vertex_array_bgra]", caps.GL_ARB_vertex_array_bgra);
        addStat("gl_caps[ARB_vertex_array_object]", caps.GL_ARB_vertex_array_object);
        addStat("gl_caps[ARB_vertex_attrib_64bit]", caps.GL_ARB_vertex_attrib_64bit);
        addStat("gl_caps[ARB_vertex_attrib_binding]", caps.GL_ARB_vertex_attrib_binding);
        addStat("gl_caps[ARB_vertex_blend]", caps.GL_ARB_vertex_blend);
        addStat("gl_caps[ARB_vertex_buffer_object]", caps.GL_ARB_vertex_buffer_object);
        addStat("gl_caps[ARB_vertex_program]", caps.GL_ARB_vertex_program);
        addStat("gl_caps[ARB_vertex_shader]", caps.GL_ARB_vertex_shader);
        addStat("gl_caps[ARB_vertex_type_10f_11f_11f_rev]", caps.GL_ARB_vertex_type_10f_11f_11f_rev);
        addStat("gl_caps[ARB_vertex_type_2_10_10_10_rev]", caps.GL_ARB_vertex_type_2_10_10_10_rev);
        addStat("gl_caps[ARB_viewport_array]", caps.GL_ARB_viewport_array);
        addStat("gl_caps[ARB_window_pos]", caps.GL_ARB_window_pos);
        
        addStat("gl_caps[EXT_Cg_shader]", caps.GL_EXT_Cg_shader);
        addStat("gl_caps[EXT_abgr]", caps.GL_EXT_abgr);
        addStat("gl_caps[EXT_bgra]", caps.GL_EXT_bgra);
        addStat("gl_caps[EXT_bindable_uniform]", caps.GL_EXT_bindable_uniform);
        addStat("gl_caps[EXT_blend_color]", caps.GL_EXT_blend_color);
        addStat("gl_caps[EXT_blend_equation_separate]", caps.GL_EXT_blend_equation_separate);
        addStat("gl_caps[EXT_blend_func_separate]", caps.GL_EXT_blend_func_separate);
        addStat("gl_caps[EXT_blend_minmax]", caps.GL_EXT_blend_minmax);
        addStat("gl_caps[EXT_blend_subtract]", caps.GL_EXT_blend_subtract);
        addStat("gl_caps[EXT_compiled_vertex_array]", caps.GL_EXT_compiled_vertex_array);
        addStat("gl_caps[EXT_depth_bounds_test]", caps.GL_EXT_depth_bounds_test);
        addStat("gl_caps[EXT_direct_state_access]", caps.GL_EXT_direct_state_access);
        addStat("gl_caps[EXT_draw_buffers2]", caps.GL_EXT_draw_buffers2);
        addStat("gl_caps[EXT_draw_instanced]", caps.GL_EXT_draw_instanced);
        addStat("gl_caps[EXT_draw_range_elements]", caps.GL_EXT_draw_range_elements);
        addStat("gl_caps[EXT_fog_coord]", caps.GL_EXT_fog_coord);
        addStat("gl_caps[EXT_framebuffer_blit]", caps.GL_EXT_framebuffer_blit);
        addStat("gl_caps[EXT_framebuffer_multisample]", caps.GL_EXT_framebuffer_multisample);
        addStat("gl_caps[EXT_framebuffer_multisample_blit_scaled]", caps.GL_EXT_framebuffer_multisample_blit_scaled);
        addStat("gl_caps[EXT_framebuffer_object]", caps.GL_EXT_framebuffer_object);
        addStat("gl_caps[EXT_framebuffer_sRGB]", caps.GL_EXT_framebuffer_sRGB);
        addStat("gl_caps[EXT_geometry_shader4]", caps.GL_EXT_geometry_shader4);
        addStat("gl_caps[EXT_gpu_program_parameters]", caps.GL_EXT_gpu_program_parameters);
        addStat("gl_caps[EXT_gpu_shader4]", caps.GL_EXT_gpu_shader4);
        addStat("gl_caps[EXT_multi_draw_arrays]", caps.GL_EXT_multi_draw_arrays);
        addStat("gl_caps[EXT_packed_depth_stencil]", caps.GL_EXT_packed_depth_stencil);
        addStat("gl_caps[EXT_packed_float]", caps.GL_EXT_packed_float);
        addStat("gl_caps[EXT_packed_pixels]", caps.GL_EXT_packed_pixels);
        addStat("gl_caps[EXT_paletted_texture]", caps.GL_EXT_paletted_texture);
        addStat("gl_caps[EXT_pixel_buffer_object]", caps.GL_EXT_pixel_buffer_object);
        addStat("gl_caps[EXT_point_parameters]", caps.GL_EXT_point_parameters);
        addStat("gl_caps[EXT_provoking_vertex]", caps.GL_EXT_provoking_vertex);
        addStat("gl_caps[EXT_rescale_normal]", caps.GL_EXT_rescale_normal);
        addStat("gl_caps[EXT_secondary_color]", caps.GL_EXT_secondary_color);
        addStat("gl_caps[EXT_separate_shader_objects]", caps.GL_EXT_separate_shader_objects);
        addStat("gl_caps[EXT_separate_specular_color]", caps.GL_EXT_separate_specular_color);
        addStat("gl_caps[EXT_shader_image_load_store]", caps.GL_EXT_shader_image_load_store);
        addStat("gl_caps[EXT_shadow_funcs]", caps.GL_EXT_shadow_funcs);
        addStat("gl_caps[EXT_shared_texture_palette]", caps.GL_EXT_shared_texture_palette);
        addStat("gl_caps[EXT_stencil_clear_tag]", caps.GL_EXT_stencil_clear_tag);
        addStat("gl_caps[EXT_stencil_two_side]", caps.GL_EXT_stencil_two_side);
        addStat("gl_caps[EXT_stencil_wrap]", caps.GL_EXT_stencil_wrap);
        addStat("gl_caps[EXT_texture_3d]", caps.GL_EXT_texture_3d);
        addStat("gl_caps[EXT_texture_array]", caps.GL_EXT_texture_array);
        addStat("gl_caps[EXT_texture_buffer_object]", caps.GL_EXT_texture_buffer_object);
        addStat("gl_caps[EXT_texture_compression_latc]", caps.GL_EXT_texture_compression_latc);
        addStat("gl_caps[EXT_texture_compression_rgtc]", caps.GL_EXT_texture_compression_rgtc);
        addStat("gl_caps[EXT_texture_compression_s3tc]", caps.GL_EXT_texture_compression_s3tc);
        addStat("gl_caps[EXT_texture_env_combine]", caps.GL_EXT_texture_env_combine);
        addStat("gl_caps[EXT_texture_env_dot3]", caps.GL_EXT_texture_env_dot3);
        addStat("gl_caps[EXT_texture_filter_anisotropic]", caps.GL_EXT_texture_filter_anisotropic);
        addStat("gl_caps[EXT_texture_integer]", caps.GL_EXT_texture_integer);
        addStat("gl_caps[EXT_texture_lod_bias]", caps.GL_EXT_texture_lod_bias);
        addStat("gl_caps[EXT_texture_mirror_clamp]", caps.GL_EXT_texture_mirror_clamp);
        addStat("gl_caps[EXT_texture_rectangle]", caps.GL_EXT_texture_rectangle);
        addStat("gl_caps[EXT_texture_sRGB]", caps.GL_EXT_texture_sRGB);
        addStat("gl_caps[EXT_texture_sRGB_decode]", caps.GL_EXT_texture_sRGB_decode);
        addStat("gl_caps[EXT_texture_shared_exponent]", caps.GL_EXT_texture_shared_exponent);
        addStat("gl_caps[EXT_texture_snorm]", caps.GL_EXT_texture_snorm);
        addStat("gl_caps[EXT_texture_swizzle]", caps.GL_EXT_texture_swizzle);
        addStat("gl_caps[EXT_timer_query]", caps.GL_EXT_timer_query);
        addStat("gl_caps[EXT_transform_feedback]", caps.GL_EXT_transform_feedback);
        addStat("gl_caps[EXT_vertex_array_bgra]", caps.GL_EXT_vertex_array_bgra);
        addStat("gl_caps[EXT_vertex_attrib_64bit]", caps.GL_EXT_vertex_attrib_64bit);
        addStat("gl_caps[EXT_vertex_shader]", caps.GL_EXT_vertex_shader);
        addStat("gl_caps[EXT_vertex_weighting]", caps.GL_EXT_vertex_weighting);
    
        addStat("gl_caps[gl_max_vertex_uniforms]", GlStateManager.glGetInteger(GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS));
        GlStateManager.glGetError();
        addStat("gl_caps[gl_max_fragment_uniforms]", GlStateManager.glGetInteger(GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS));
        GlStateManager.glGetError();
        addStat("gl_caps[gl_max_vertex_attribs]", GlStateManager.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS));
        GlStateManager.glGetError();
        addStat("gl_caps[gl_max_vertex_texture_image_units]", GlStateManager.glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS));
        GlStateManager.glGetError();
        addStat("gl_caps[gl_max_texture_image_units]", GlStateManager.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS));
        GlStateManager.glGetError();
        addStat("gl_caps[gl_max_array_texture_layers]", GlStateManager.glGetInteger(GL30.GL_MAX_ARRAY_TEXTURE_LAYERS));
        GlStateManager.glGetError();
    }
    
    private static boolean isJvm64bit() {
        return Stream.of("sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch")
            .map(System::getProperty)
            .anyMatch(s -> StringUtils.contains(s, "64"));
    }
    
    private Optional<String> getOptiFineVersion() {
        try {
            Class.forName("optifine.OptiFineTweaker");
            // exception not thrown, OptiFine exists
            
            Class<?> optifineConfig = Class.forName("Config"); // why isn't this class in a package?!
            Field versionField = optifineConfig.getDeclaredField("VERSION");
            versionField.setAccessible(true);
            return Optional.of((String) versionField.get(null));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }
    
    private Optional<String> getFoamFixVersion() {
        Map<String, ModContainer> indexedModList = Loader.instance().getIndexedModList();
        if (Loader.isModLoaded("foamfix") && indexedModList.containsKey("foamfix")) {
            ModContainer modContainer = indexedModList.get("foamfix");
            return Optional.of(modContainer.getVersion());
        } else {
            return Optional.empty();
        }
    }
    
    public Map<String, String> getStats() {
        return stats;
    }
}
