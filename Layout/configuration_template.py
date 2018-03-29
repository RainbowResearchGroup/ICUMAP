from PreProcessor import *
from sys import maxint
# noinspection PyUnresolvedReferences
from Parameter import Parameter

# True to generate a layout; False to skip this phase.
run_tsne = True

# True to convert a generated layout into ICUMAP Visualisation Module format; False to skip this phase.
generate_json = True

# File name that each shard starts with e.g. "../data/fv-"
shards_prefix = "path/to/shards/shard-"

min_shard_id = 0
max_shard_id = maxint
shard_interval = 100  # If using MetaVision module data, this should be the same as "batchSize".

# Directory in which to save working state. This can be a temporary directory, in which case you will need to run
# the full tSNE algorithm every time, or a permanent directory, which allows caching t-SNE results.
intermediate_directory = ""

# File describing the parameters available in the data CSVs.
parameters_file = "path/to/parameters.csv"

# Directory to write output files to.
# For use with the ICUMAP Visualisation Module, place in ICUMAP/Visualisation/src/t-sne-app/data/data-files/<run_name>
output_directory = "path/to/ICUMAP/Visualisation/src/t-sne-app/data/data-files/test-run/"

# Describes the pre-processing steps required to produce data for writing patient data to json.
#
# This must create two files in the intermediate directory: "csv.csv", and "all.csv", as in the example below.
# For example, to exclude patients with missing parameters, impute, withhold the most recent patients, and keep only
# 30k data points to pass to t-SNE, use the following pre-processor:
#  (PreProcessor
#      .program(FilterPatientsMissingFeatures())
#      .then(SaveState(intermediate_directory + "csv.csv"))
#      .then(ImputeForwardFill())
#      .then(SaveState(intermediate_directory + "all.csv"))
#      .then(DropRowsAfterPatientId(4000))
#      .then(TakeBottomNRows(30000)))
# To create a custom PreProcessor step, extend PreProcessorCommand in PreProcessor.py.
json_pre_processor = PreProcessor()

# Describes pre-processing steps required to produce a csv consumable by bhtsne.
# Normally, this just means running the json_pre_processor, then  scaling the input features, and annotating the ids,
# as given in the example here.
# This must produce a CSV called data.csv, placed in the intermediate directory.
pre_processor = \
    (json_pre_processor
     .then(ScaleFeatures())
     .then(AnnotateIds())
     .then(SaveState(intermediate_directory + "data.csv")))

# List of Parameter objects. Contains one object for each parameter to be used by tsne_main.py.
# Refer to the documentation in Parameter.py for more details.
# For example: [Parameter("Heart Rate", "numeric", True, True, True), ...]
# Do not include Patient Id and time. These will be added automatically.
parameter_properties = []

# Number of iterations to run t-SNE for.
target_iteration = 800

# Length-shortening factor. A value of 1 is equivalent to Euclidean distance. The smaller the value, the smaller the
# distance patients will move over the dimension-reduced space during their admission.
# For 12-15 variables, values around 0.3 worked well for the databases in our study.
# When set to 1, an optimised Euclidean algorithm is used, so will be much faster. It may be useful to set to 1 to
# get an idea of the likely output structure, then try smaller values if that looks good. This is an especially useful
# technique when clustering.
tsne_alpha = 1

# Distance between variables tagged as cardinal in parameter_properties. The same distance applies to all cardinal
# variables.
cardinal_distance = 50

# Set this property to initialise t-SNE's randomisation algorithm with the given seed. This ensures subsequent runs
# will generate the same layout if given the same input data. Otherwise, a random seed will be used.
# rand_seed = 20
