import shutil
import time
import json_output
import tsne_generate
import os
import imp
import sys


def import_from_uri(uri, absl=False):
    if not absl:
        uri = os.path.normpath(os.path.join(os.path.dirname(__file__), uri))
    path, fname = os.path.split(uri)
    mname, ext = os.path.splitext(fname)

    no_ext = os.path.join(path, mname)

    if os.path.exists(no_ext + '.py'):
        return imp.load_source(mname, no_ext + '.py')


if __name__ == "__main__":
    if len(sys.argv) <= 1:
        print("Error: Please provide a configuration file to use.")
        sys.exit()

    configuration_path = sys.argv[1]
    configuration = import_from_uri(configuration_path)

    if configuration is None:
        print("Error: Unable to load configuration file.")
        sys.exit()

    parameter_properties = configuration.parameter_properties
    input_parameter_names = map(lambda p: p.name, filter(lambda p: p.includeInLayout, parameter_properties))
    run_name = configuration.run_name
    run_tsne = configuration.run_tsne
    intermediate_directory = configuration.intermediate_directory
    interest_parameter_names = ["Patient Id", "time"]
    shards_prefix = configuration.shards_prefix
    parameters_file = configuration.parameters_file
    pre_processor = configuration.pre_processor
    json_pre_processor = configuration.json_pre_processor
    tsne_alpha = configuration.tsne_alpha
    min_shard_id = configuration.min_shard_id
    max_shard_id = configuration.max_shard_id
    shard_interval = configuration.shard_interval
    generate_json = configuration.generate_json
    output_directory = configuration.output_directory
    target_iteration = getattr(configuration, 'target_iteration', 0)
    generate_iteration = getattr(configuration, 'generate_iteration', target_iteration)
    output_iteration = getattr(configuration, 'output_iteration', target_iteration)
    cardinal_distance = getattr(configuration, 'cardinal_distance', 0)
    rand_seed = getattr(configuration, 'rand_seed', -1)

    if generate_json and not run_tsne and not os.path.exists(intermediate_directory):
        print("Error: run_tsne is set to False but the specified intermediate_directory could not be found.")
        sys.exit()

    print("Starting run: " + run_name)

    keep_parameter_names = list(input_parameter_names)
    for p in interest_parameter_names:
        keep_parameter_names.append(p)

    if run_tsne:
        if os.path.exists(intermediate_directory):
            shutil.rmtree(intermediate_directory)

        os.makedirs(intermediate_directory)
        shutil.copy(configuration_path, intermediate_directory + "config.py")

        tsne_generate.run(shards_prefix, intermediate_directory, input_parameter_names,
                          keep_parameter_names, parameters_file, pre_processor, generate_iteration, tsne_alpha,
                          min_shard_id, max_shard_id, shard_interval, cardinal_distance, rand_seed)
        while not os.path.isfile(intermediate_directory + str(generate_iteration) + ".dat"):
            time.sleep(1)
    else:
        shutil.copy(configuration_path, intermediate_directory + "config.py")
        print("Skipping t-sne data generation")

    if generate_json:
        if os.path.exists(output_directory):
            shutil.rmtree(output_directory)
        json_output.run(intermediate_directory, output_directory, input_parameter_names, keep_parameter_names,
                        output_iteration, json_pre_processor, parameter_properties)

        shutil.copy(intermediate_directory + "config.py", output_directory + "config.py")
    else:
        print("Skipping json output")

    print("Generation complete for " + run_name)
    print("")
