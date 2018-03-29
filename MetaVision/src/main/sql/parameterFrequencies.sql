SELECT COUNT(DISTINCT Signals.PatientID) as Frequency, Signals.ParameterID, ParameterName
FROM Signals
  JOIN Parameters ON Signals.ParameterID=Parameters.ParameterID
  JOIN Patients ON Patients.PatientID=Signals.PatientID
GROUP BY Signals.ParameterID, ParameterName
ORDER BY Frequency Desc