# ICUMAP Layout (t-SNE) Module
This module takes patient feature vectors and uses trajectory-optimised t-SNE to generate a list of co-ordinates in 2D
space for each patient. It outputs json files in the format required by the ICUMAP visualisation module.


## Installation
### 1. Compile bhtsne
The layout is generated using a modified version of the Barnes-Hut t-SNE algorithm. 
Compilation instructions are the same as those for the implementation by the original BH t-SNE authors. 
That implementation is available [here](https://github.com/lvdmaaten/bhtsne/), from where these instructions are copied:

On Linux or OS X, compile the source using the following command:

```
g++ sptree.cpp tsne.cpp tsne_main.cpp -o bh_tsne -O2
```

The executable will be called `bh_tsne`.

On Windows using Visual C++, do the following in your command line:

- Find the `vcvars64.bat` file in your Visual C++ installation directory. This file may be named `vcvars64.bat` or something similar. For example:

```
  // Visual Studio 12
  "C:\Program Files (x86)\Microsoft Visual Studio 12.0\VC\bin\amd64\vcvars64.bat"

  // Visual Studio 2013 Express:
  C:\VisualStudioExp2013\VC\bin\x86_amd64\vcvarsx86_amd64.bat
```

- From `cmd.exe`, go to the directory containing that .bat file and run it.

- Go to `bhtsne` directory and run:

```
  nmake -f Makefile.win all
```

The executable will be called `windows\bh_tsne.exe`.

### 2. Install Python Dependencies 
1. Install [Python 2.7](https://www.python.org/downloads/).
2. Module dependencies are managed using [pipenv](https://github.com/pypa/pipenv). 
   Install this using ```$ pip install pipenv --user```. 
   Further support on this is available at [python.org](https://packaging.python.org/tutorials/managing-dependencies/#managing-dependencies).
3. Install dependencies from the locked Pipfile with ```$ pipenv install --ignore-pipfile```.

## Usage
All commands to be run from this directory, unless otherwise specified.

### 1. Prepare Input Files
The t-SNE module requires files in the following formats.

If using a MetaVision 5 database as the data source, follow the instructions in the ICUMAP MetaVision module. This will
generate files in the formats described below.

#### 1. A CSV (or CSVs) containing patient features.
Each row of each CSV should be in the following format:

| Parameter 1 |   ...  | Parameter n | Patient Id | Time-Step | 
|-------------|--------|-------------|------------|-----------|

Note:
- The CSVs should not include a header row.
- Rows must be sorted in ascending order of Patient Id then Time-Step
- Use NaN to represent missing data.
- The Patient Id and Time-Step columns cannot contain NaN.

Data may be provided in one CSV or split across many. If splitting across multiple files:
- Each CSV must end with ```$lowestPatientId.csv``` where ```lowestPatientId``` is the id of the first patient in that
file. For example, if a file contains patients 3100 to 3150, the file may be called ```data-3100.csv```.
- Individual patients cannot be split across files.
- Each file must cover the same size of interval between minimum and maximum Patient Ids.

#### 2. A file describing the parameters in the above CSV(s).
If you extracted data using ICUMAP's MetaVision module you may use the same ```parameters.csv``` file without modification.

Otherwise, produce a CSV with the following columns:

| Blank | Parameter Name | 
|-------|----------------|

TODO: Are any other columns needed here?

### 2. Configure
ICUMAP t-SNE is configured by setting properties in a Python configuration file.

A documented template of such a file is available at ```./configuration_template.py```

### 3. Execute
```$ pipenv run python tsne_main.py path/to/configuration_file.py```

### 4. Output
This creates two new files, which are saved in the specified output directory, namely ```queries.json``` 
and ```data.json```.
It also copies the original configuration file to this directory, for reference purposes.

`data.json` contains the information required to plot the cloud, sidebar, and histograms.
`queries.json` contains the patients that are not in the "historical" set, i.e. those patients that are not in the cloud.