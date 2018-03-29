import os

import pandas as pd

from PreProcessor import *
from bhtsne import bhtsne


class Parameter:
    def __init__(self, name, index, cardinal):
        self.name = name
        self.index = index
        self.cardinal = cardinal


def load_parameters(parameters_file):
    lines = pd.DataFrame.from_csv(parameters_file, header=None, index_col=None).as_matrix()
    parameters = {}
    for i, line in enumerate(lines):
        name = line[1]
        cardinal = line[2] == "cardinal" or line[2].startswith("icm-")
        parameters[name] = Parameter(name, i, cardinal)
    return parameters


def csv_from_shards(shards_prefix, parameters, keep_parameter_names, min_shard, max_shard, interval):
    csv = np.genfromtxt(shards_prefix + str(min_shard) + ".csv", delimiter=",")
    csv = csv[:, map(lambda f: parameters[f].index, keep_parameter_names)]

    for i in range(min_shard / interval + 1, max_shard / interval):
        print("Loading data: " + str(float(i * interval - min_shard) / (max_shard - min_shard) * 100))
        temp = np.genfromtxt(shards_prefix + str(i * interval) + ".csv", delimiter=",")
        temp = temp[:, map(lambda f: parameters[f].index, keep_parameter_names)]
        csv = np.concatenate((csv, temp), axis=0)
    print("Loading data: 100.0")

    return csv


def patient_count(csv, parameters):
    print(parameters["Patient Id"].index)
    print(np.unique(csv[parameters["Patient Id"].index]))

    patient_ids = csv[parameters["Patient Id"].index]
    return len(np.unique(patient_ids))


def run(shards_prefix, intermediate_directory, input_parameter_names, keep_parameter_names, parameters_file,
        pre_processor, iterations, tsne_alpha, min_shard_id, max_shard_id, shard_interval, cardinal_distance, seed=-1):
    if not os.path.exists(intermediate_directory):
        os.makedirs(intermediate_directory)

    # Constants which could be pulled out elsewhere
    pre_processor_progress_interval = 20000
    perplexity = 50

    # Main program
    parameters = load_parameters(parameters_file)
    raw_csv = csv_from_shards(shards_prefix, parameters, keep_parameter_names,
                              min_shard_id, max_shard_id, shard_interval)

    np.savetxt(intermediate_directory + "/selected.csv", raw_csv, delimiter=",", fmt="%10.8f")

    pre_processor_parameters = {}
    for i, line in enumerate(keep_parameter_names):
        pre_processor_parameters[line] = i
    csv = pre_processor.run(raw_csv, pre_processor_parameters, input_parameter_names,
                            pre_processor_progress_interval)

    input_parameter_indices = map(lambda x: pre_processor_parameters[x], input_parameter_names)
    input_parameter_indices.append(csv.shape[1] - 2)
    input_parameter_indices.append(csv.shape[1] - 1)
    tsne_input = csv[:, input_parameter_indices]
    parameters_cardinal = map(lambda name: parameters[name].cardinal, input_parameter_names)

    print(parameters_cardinal)

    bhtsne.run_bh_tsne(
        data=tsne_input,
        intermediate_directory=intermediate_directory,
        no_dims=2,
        parameters=parameters_cardinal,
        perplexity=perplexity,
        verbose=True,
        init_layout=None,
        use_pca=False,
        max_iter=iterations,
        alpha=tsne_alpha,
        death_distinct=False,
        beta=1.0,
        cardinal_distance=cardinal_distance,
        randseed=seed
    )
