# ICUMAP MetaVision Module
This module provides code to help extract data from a MetaVision 5 database, and to convert this data into the input 
format required by the ICUMAP t-SNE module.

## Summary of Database Structure
Configuration of this module may require basic familiarity with the underlying database structure.
Knowledge of SQL and Scala are assumed.
A brief summary of the most important tables is given here:

| Table | Contents |
|-------|----------|
| **Patients** | Lists the ids of all patients in the database, as well as their admission/discharge dates, names, hospital numbers etc.
| **Parameters** | This gives the name, id, unit id, and data type of every parameter which the hospital unit has created. A parameter is any variable the unit captures e.g. heart rate, nursing notes, diagnosis code etc.
| **\*Signals** | All the actual measurements are recorded in tables ending with the term "signals". Each signals table records the values for a particular data type - for example, all multiple choice parameters are in TextSignals, all free text parameters are in FreeTextSignals, and all sensor data should be in Signals. The data format is (patient id, parameter id, time, value, warning, error). Warning or Error are set to 0 if false, 1 otherwise, by nurses when they manually validate data. A lot of the data collected at high frequency will not be validated.
| **t_ParametersType** | Maps unit ids to unit descriptions. Use this information to find the table which a parameter's values are located in. For example, if the data type is free text, the recorded values will be stored in FreeTextSignals.
| **ParametersText** | Lists all the options available for each multiple choice parameter.
| **Units** | Gives the unit name of each unit (e.g. ml, bpm). Data in the signals table is not always stored in this unit - convert to this unit using `(Signals.Value - Units.Addition) / Units.Multiplier`.


## Setup and Usage
All commands are to be run from this directory (ICUMAP/database-extractors), unless otherwise specified.

These instructions assume that a MetaVision database archive is available on an accessible SQL Server.

### 1. Install sbt
This project was developed using sbt version 1.1.1. 

sbt is available from [scala-sbt.org](https://www.scala-sbt.org/)

### 2. Prepare Parameters File

This is a CSV which tells the program which parameters to extract, where to find those parameters, 
and how each parameter should be interpreted. 

An example of a CSV file in this format is given [at the end of this section](#example-of-a-parameters.csv).

#### Format
Each row of the CSV should contain the following columns:

| Parameter Id | Parameter Name | Data Type | Argument (Optional) |
|--------------|----------------|-----------|---------------------|

where the columns are used for the following:

| Column | Description of Contents |
|--------|-------------------------|
| Parameter Id | Unique Id which MetaVision assigned to this parameter. This is _probably_ a 4-digit number.
| Parameter Name | A human friendly name for this parameter. This does not need to be the same as the name used in the database, **except for Patient Id and time**
| Data Type | Describes where this variable is stored and how it should be interpreted. For options, see the [Data Type Options table](#data-type-options) below.
| Argument | Some data types may require an additional piece of data to function. Put that data here.

Additional columns will be ignored. Do not provide a header row. 
Note that this file MUST end with parameters named "Patient Id" and "time".

The output of the SQL query provided in ```src/sql/parameterFrequencies.sql``` may be a useful starting point when
constructing this file.

#### Data Type Options
Extractors for the following data types are provided. 
Assuming the unit's database is reasonably configured, you should not generally have to worry about which table
the relevant data is in.

| Data Type | Description | MetaVision Source Table |
|-----------|-------------|-------------------------|
| sensor | For machine-generated, continuous data like heart rate. | Signals
| ordinal | Parse a multiple-choice input as a discrete scale, using the Value assigned to this option in ParametersText. Provide the optional argument "constant" to use the same value at all time steps. If "constant" is set and this parameter has multiple entries, the most recent will be used. | TextSignals, ParametersText 
| cardinal | Parse a multiple-choice input as cardinal options. You may provide the optional argument "constant" similarly to ordinal. | TextSignals
| drug | Outputs 1 if this drug was being administered to this patient at this time, 0 otherwise. | RangeSignals
| died | Outputs 1 if this patient died, 0 otherwise, using a multiple-choice parameter. Requires the TextID of the option representing died as as additional argument. For example, if using the parameter discharge destination where "ward" has TextId 1 and "mortuary" has TextId 2, the argument should be 2. | TextSignals
| icm-type | Parse as an ICNARC code, and return the type (surgical/non-surgical) digit. | FreeTextSignals
| icm-system | Parse as an ICNARC code, and return the system digits. | FreeTextSignals
| icm-site | Parse as an ICNARC code, and return the site digits. | FreeTextSignals
| icm-process | Parse as an ICNARC code, and return the process digits. | FreeTextSignals
| icm-condition | Parse as an ICNARC code, and return the condition digits. | FreeTextSignals
| patient-id | Writes the MetaVision-assigned id for this patient. Does not require a parameter id. | N/A
| time-step | Writes the Time-Step for which the readings from this row are extracted. Does not require a parameter  id. | N/A 

If you need an extractor not provided above, implement a new extractor in ```Extractors.scala```, and submit a pull request.

#### Example of a parameters.csv
As an example, you might provide the CSV:

| | | | |
|---|---|---|---|
| 1234 | Heart Rate | sensor
| 1235 | Blood Pressure | sensor
| 1236 | Urine Output | sensor
| 2345 | Glasgow Coma Score | ordinal 
| 2357 | Type of Surgery | cardinal | constant
| 4180 | Died | died | 3 
| | Patient Id | patient-id
| | time | time-step

The t-SNE module allows for further sub-selection of variables, so there is no need to extract the minimum set that
will ultimately be used here. In fact, it may be useful to extract more variables than will ultimately be used in
order to make experimenting with layouts easier later on.

### 3. Configure
The CSV produced in the previous step only describes which features to extract.

To configure the remaining options (which attributes to extract, which patients to extract, which database etc.), 
set the ```config``` val in ```ExtractEverything.scala```. For details, refer to the ScalaDoc 
for ```ExtractorConfiguration.scala```. 

### 4. Compile and Run
Run ```$ sbt run```.

sbt will acquire the correct version of Scala and all dependencies, then compile the source code and run 
the program.

The program will save a JSON attributes file and CSV(s) to the specified output locations. The output files are saved
in the format required by the ICUMAP t-SNE module.
