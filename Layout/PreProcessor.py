import numpy as np
from sklearn import preprocessing


class PreProcessorCommand:
    def __init__(self):
        pass
    
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        """
        Run a pre-processing step on the csv.
        :param csv: The CSV to run this pre-processing step on. Each row should contain a patient id, time step, and
                    values for several parameters. The CSV should be sorted by patient id then time step.
        :param features: Dictionary of parameter to column index for all columns.
        :param input_features: Dictionary of parameter to column index for the columns to be used in the t-SNE layout
                               generation.
        :param pre_processor_progress_interval: Number of rows to process between printing progress updates.
        :return: CSV produced by running this pre-processing step on the input csv.
        """
        pass


class ConstrainTimeRange(PreProcessorCommand):
    def __init__(self, min_time, max_time):
        """
        Removes elements which do not occur within the time-steps min_time and max_time.
        Also shifts all time-steps such that t=min_time+k is relabelled t=k
        """
        PreProcessorCommand.__init__(self)
        self.min_time = min_time,
        self.max_time = max_time
        
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        row = 0
        late_row_indices = []
        while row < csv.shape[0]:
            if pre_processor_progress_interval is not None and \
                                    row % pre_processor_progress_interval == 0:
                print("Constraining range: " + str(100.0 * row / csv.shape[0]))
            if self.min_time <= csv[row, features["time"]] < self.max_time:
                csv[row, features["time"]] -= self.min_time
                late_row_indices.append(row)
            row += 1
        csv = csv[late_row_indices]
        print("Constraining range: 100.0")
        return csv
    
    
class TakeTopNRows(PreProcessorCommand):
    def __init__(self, rows):
        PreProcessorCommand.__init__(self)
        self.rows = rows
    
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Selecting top " + str(self.rows) + " rows")
        row = min(self.rows, csv.shape[0]) - 1
        patient_id = csv[row, features["Patient Id"]]
        prev_patient_id = csv[row - 1, features["Patient Id"]]
        while patient_id == prev_patient_id:
            row -= 1
            patient_id = csv[row, features["Patient Id"]]
            prev_patient_id = csv[row - 1, features["Patient Id"]]
        csv = csv[0:row]
        print("Selected top " + str(row) + " rows")
        return csv
    
    
class TakeBottomNRows(PreProcessorCommand):
    def __init__(self, rows):
        PreProcessorCommand.__init__(self)
        self.rows = rows

    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Selecting bottom " + str(self.rows) + " rows")
        row = max(0, csv.shape[0] - self.rows)
        while csv[row, features["time"]] != 0:
            row += 1
        if row < csv.shape[0]:
            csv = csv[row:csv.shape[0]]
        else:
            csv = csv[0:0, :]
        print("select bottom " + str(csv.shape[0] - row) + " rows")
        return csv


class DropBottomNPatients(PreProcessorCommand):
    def __init__(self, patients):
        PreProcessorCommand.__init__(self)
        self.patients = patients

    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Dropping bottom " + str(self.patients) + " rows")
        dropped_patients = 0
        while dropped_patients < self.patients:
            if csv[csv.shape[0] - 1, features["time"]] == 0:
                dropped_patients += 1
            csv = csv[:csv.shape[0] - 1, :]
        return csv


class DropRowsAfterPatientId(PreProcessorCommand):
    def __init__(self, patient_id):
        PreProcessorCommand.__init__(self)
        self.patient_id = patient_id

    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Dropping patients with id >= " + str(self.patient_id))
        indices = []
        row = 0
        while row < csv.shape[0]:
            if csv[row, features["Patient Id"]] < self.patient_id:
                indices.append(row)
            row += 1
        csv = csv[indices, :]
        return csv


class DeleteInvalidValues(PreProcessorCommand):
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        for f in input_features:
            print("Delete invalid features for " + f)
            col_id = features[f]
            col = csv[:, col_id]
            col = col[~np.isnan(col)]
            iq25 = np.percentile(col, 25)
            iq75 = np.percentile(col, 75)
            iqr = (iq75 - iq25)
            min_valid = iq25 - 1.5 * iqr
            max_valid = iq75 + 1.5 * iqr
            row = 0
            while row < csv.shape[0]:
                if csv[row, col_id] < min_valid or csv[row, col_id] > max_valid:
                    csv[row, col_id] = np.nan
                row += 1
        return csv
    
   
# TODO: This script is being clever and doing multiple things at once (removing patients with missing columns,
# TODO: patients with missing time steps, patients whose time doesn't start from zero). Refactor this
# TODO: to separate the different tasks being completed here.
class FilterPatientsMissingFeatures(PreProcessorCommand):
    """Removes patients which have no data at any time for a particular feature"""
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print(csv.shape)
        # Delete invalid rows
        valid_indices = []
        row = 0
        last_patient_id = -1
        print(features, input_features)
        while row < csv.shape[0]:
            if row != 0:
                last_patient_id = csv[row - 1, features["Patient Id"]]
            current_patient_id = csv[row, features["Patient Id"]]
            if row % pre_processor_progress_interval == 0:
                print("Invalid row deletion: " + str(100.0 * row / csv.shape[0]))
            if current_patient_id != last_patient_id:
                # Found a new patient to examine!
                # Test that every column has a feature somewhere.
                has_features = map(lambda x: False, input_features)
                scan_row = row
                for j, feature_index in enumerate(map(lambda x: features[x], input_features)):
                    scan_row = row
                    scan_patient_id = current_patient_id
                    while scan_patient_id == current_patient_id:
                        if not np.isnan(csv[scan_row, feature_index]):
                            # This feature has a value somewhere for this patient
                            has_features[j] = True
                        scan_row += 1
                        if scan_row > csv.shape[0] - 1:
                            scan_patient_id = -1
                        else:
                            scan_patient_id = csv[scan_row, features["Patient Id"]]
            
                # Skip the current patient if they have an empty feature column or their time doesn't start from 0,
                # or they don't have a time = 4 (latter case is because 4 points are needed to be able to compute
                # a 4 hour moving average)
                if False in has_features or csv[row, features["time"]] != 0:
                    row = scan_row
                else:
                    valid_indices.append(row)
                    row += 1
            else:
                # Same patient as last time
                last_time = csv[row - 1, features["time"]]
                current_time = csv[row, features["time"]]
                if current_time == last_time + 1:  # Sequential time increase
                    valid_indices.append(row)
                    row += 1
                else:  # non-sequential time increase
                    # This row is not valid, and neither are all remaining rows for this patient
                    # TODO: Further investigation to make sure this is a sensible thing to do.
                    # TODO: I think it is because if there is a significant jump it's because of lab
                    # TODO: results returned after discharge. But need to check this.
                    while current_patient_id == last_patient_id:
                        row += 1
                        if row > csv.shape[0] - 1:
                            current_patient_id = -1
                        else:
                            current_patient_id = csv[row, features["Patient Id"]]
        csv = csv[valid_indices]
        print("Invalid row deletion: 100.0")
        print(csv.shape)
        return csv
    

class MovingAverage(PreProcessorCommand):
    def __init__(self, window_size, step_size=None):
        """Computes a moving average over the specified number of time-steps"""
        PreProcessorCommand.__init__(self)
        self.window_size = window_size
        self.step_size = step_size

    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        input_features_csv_indices = map(lambda x: features[x], input_features)
        row = 0
        ma_indices = []
        last_patient_id = -1
        while row < csv.shape[0]:
            if pre_processor_progress_interval is not None and \
                                    row % pre_processor_progress_interval == 0:
                print("Moving average: " + str(100.0 * row / csv.shape[0]))
            current_patient_id = csv[row, features["Patient Id"]]
            if current_patient_id == last_patient_id:
                for fi in input_features_csv_indices:
                    total = 0
                    div = 0.0
                    for j in range(0, self.window_size):
                        if not np.isnan(csv[row - j, fi]):
                            total += csv[row - j, fi]
                            div += 1.0
                    if div == 0.0:
                        csv[row - (self.window_size - 1), fi] = np.nan
                    else:
                        csv[row - (self.window_size - 1), fi] = total / div
                ma_indices.append(row - (self.window_size - 1))
                row += self.step_size
            else:
                row += (self.window_size - 1)
            last_patient_id = current_patient_id
        csv = csv[ma_indices, :]
        print("Moving average: 100.0")
        return csv


class SaveState(PreProcessorCommand):
    def __init__(self, output_file):
        PreProcessorCommand.__init__(self)
        self.output_file = output_file

    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Saving state to " + self.output_file)
        np.savetxt(self.output_file, csv, delimiter=",", fmt="%10.8f")
        print("Saved state.")
        return csv
    

class ScaleFeatures(PreProcessorCommand):
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        print("Normalising features.")
        input_features_csv_indices = map(lambda x: features[x], input_features)
        csv[:, input_features_csv_indices] = \
            preprocessing.RobustScaler().fit_transform(csv[:, input_features_csv_indices])
        print("Normalised features.")
        return csv


class ImputeForwardFill(PreProcessorCommand):
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        row = 0
        while row < csv.shape[0]:
            if row % pre_processor_progress_interval == 0:
                print("Forward fill: " + str(100.0 * row / csv.shape[0]))
            time = csv[row, features["time"]]
            if time == 0:
                for j, feature in enumerate(map(lambda x: csv[row, features[x]], input_features)):
                    feature_index = features[input_features[j]]
                    if np.isnan(feature):
                        scan_row = row + 1
                        while np.isnan(csv[scan_row, feature_index]):
                            scan_row += 1
                        csv[row, feature_index] = csv[scan_row, feature_index]
            else:
                for j, feature in enumerate(map(lambda x: csv[row, features[x]], input_features)):
                    feature_index = features[input_features[j]]
                    if np.isnan(feature):
                        csv[row, feature_index] = csv[row - 1, feature_index]
            row += 1
        print("Forward fill: 100.0")
        return csv
    
    
class AnnotateIds(PreProcessorCommand):
    def execute(self, csv, features, input_features, pre_processor_progress_interval):
        # Append some new columns
        r, c = csv.shape
        csv = np.c_[csv, np.zeros(r), np.zeros(r)]
        
        row = 0
        while row < csv.shape[0] - 1:
            if row % pre_processor_progress_interval == 0:
                print("Annotate trajectory ids: " + str(100.0 * row / csv.shape[0]))
            csv[row, c] = row + 1  # row + 1 because 0 is used if no trajectory
            if csv[row, features["Patient Id"]] == csv[row + 1, features["Patient Id"]]:
                # Assume time is in ascending order, therefore set next id for this element
                # to be the next row (i.e. row + 1 (+ 1 because 0 is for no trajectory (giving row + 2)))
                csv[row, c + 1] = row + 2
            row += 1
        csv[row, c] = row + 1
        print("Annotate trajectory ids: 100.0")
        return csv


class PreProcessor:
    def __init__(self, program=None):
        if program is None:
            program = []
        self._program = program

    @staticmethod
    def program(ppc):
        # type: (PreProcessorCommand) -> PreProcessor
        return PreProcessor().then(ppc)

    def then(self, ppc):
        # type: (PreProcessorCommand) -> PreProcessor
        new_program = list(self._program)
        new_program.append(ppc)
        return PreProcessor(new_program)

    def run(self, csv, features, input_features_csv_indices, pre_processor_progress_interval):
        for ppc in self._program:
            csv = ppc.execute(csv, features, input_features_csv_indices, pre_processor_progress_interval)
        return csv
