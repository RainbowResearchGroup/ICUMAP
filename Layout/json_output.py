import json
import os
from collections import OrderedDict

from PreProcessor import *
from bhtsne import bhtsne


def load_csvs(directory):
    return (
        np.genfromtxt(directory + "data.csv", delimiter=","),
        np.genfromtxt(directory + "csv.csv", delimiter=","),
        np.genfromtxt(directory + "all.csv", delimiter=",")
    )


def parameters_from_keep_parameters(keep_features):
    features = OrderedDict()
    for i, line in enumerate(keep_features):
        features[line] = i
    return features


def load_tsne_output(directory, iteration):
    res = []
    for result in bhtsne.read_results(directory + str(iteration) + ".dat"):
        sample_res = []
        for r in result:
            sample_res.append(r)
        res.append(sample_res)
    tsne_output = np.asarray(res, dtype='float64')
    return tsne_output


def write_json_file(output_path, tsne_xy, ids, next_ids, csv, parameters, patient_id_index, times, scaler,
                    parameter_properties):
    patients = []

    row = 0
    while row < tsne_xy.shape[0]:
        patients.append({
            "x": tsne_xy[row][0],
            "y": tsne_xy[row][1],
            "id": ids[row] - 1,
            "next": next_ids[row] - 1,
            "prev": -1 if row == 0 or patients[-1]["next"] == -1 else patients[-1]["id"],
            "time": times[row],
            "patientId": csv[row, patient_id_index],
            "inputFeatures":
                map(
                    lambda k: csv[row, parameters[k]],
                    parameters.keys()
                ),
        })
        row += 1

    out = {
        "inputFeatureNames": OrderedDict(map(
            lambda (i, k): (k if k != "time" else "Time Step", i), enumerate(parameters.keys())
        )),
        "scaler": {
            "center": list(scaler.center_),
            "scale": list(scaler.scale_)
        },
        "patients": patients,
        "featureProperties": map(lambda p: p.__dict__, parameter_properties)
    }

    print("Writing to: " + output_path)
    with open(output_path, 'w') as f:
        json.dump(out, f)


def write_query_patients(output_path, query_csv, features, patient_id_index, times):
    out = []

    row = query_csv.shape[0] - 1
    query_count = 0
    while row >= 0:
        patient = {
            "patientId": query_csv[row, patient_id_index],
            "time": times[row],
            "rawFeatures":
                map(
                    lambda k:
                    "null" if np.isnan(query_csv[row, features[k]])
                    else query_csv[row, features[k]],
                    features.keys()
                )
        }
        out.append(patient)
        if times[row] == 0:
            query_count += 1
        row -= 1

    out.reverse()
    with open(output_path, 'w') as f:
        json.dump(out, f)


def run(intermediate_directory, output_directory, input_parameters, keep_parameters, iteration, pre_processor,
        parameter_properties):
    if not os.path.exists(output_directory):
        os.makedirs(output_directory)

    print("Loading csvs...")
    tsne_csv, csv, all_csv = load_csvs(intermediate_directory)
    tsne_output = load_tsne_output(intermediate_directory, iteration)

    parameters = parameters_from_keep_parameters(keep_parameters)
    patient_id_index = parameters["Patient Id"]

    print("Determining scaler parameters")
    scale_cols = map(lambda k: parameters[k], parameters.keys())

    csv = pre_processor.run(csv, parameters, input_parameters, 20000)
    scaler = preprocessing.RobustScaler().fit(csv[:, scale_cols])

    # Uncomment to disable imputed values in the queries.json file.
    # tsne_csv, csv, all_csv = load_csvs(directory)

    print("Generating data.json")
    parameters = parameters_from_keep_parameters(input_parameters)
    parameters["time"] = csv.shape[1] - 1
    write_json_file(
        output_directory + "data.json",
        tsne_output,
        tsne_csv[:, tsne_csv.shape[1] - 2],
        tsne_csv[:, tsne_csv.shape[1] - 1],
        tsne_csv,
        parameters,
        patient_id_index,
        tsne_csv[:, tsne_csv.shape[1] - 3],
        scaler,
        parameter_properties
    )

    # Query CSV contains only patients with ids higher than those in csv
    max_data_patient = csv[:, patient_id_index].max()
    query_csv = all_csv[all_csv[:, patient_id_index] > max_data_patient]

    print("Generating queries.json")
    write_query_patients(
        output_directory + "queries.json",
        query_csv,
        parameters,
        patient_id_index,
        query_csv[:, query_csv.shape[1] - 1]
    )
