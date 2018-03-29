class Parameter:

    def __init__(self, name, type, include_in_layout, include_in_similarity, show_in_sidebar):
        """
        :param name:                  Name of this parameter, as given in the parameters CSV e.g. "Heart Rate"
        :param type:                  Either "numeric" or "cardinal" - used to determine how to compute similarity
                                      for this parameter.
        :param include_in_layout:     Whether this parameter should influence the projected layout.
        :param include_in_similarity: Whether this parameter should be used by the visualisation module's nearest
                                      patients calculation.
        :param show_in_sidebar:       Whether this parameter should be listed in the visualisation's sidebar,
                                      with a histogram of values.
        """
        self.name = name
        self.type = type
        self.includeInLayout = include_in_layout
        self.includeInSimilarity = include_in_similarity
        self.showInSidebar = show_in_sidebar
