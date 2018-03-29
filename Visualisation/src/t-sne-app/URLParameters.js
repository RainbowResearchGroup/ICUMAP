class URLParameters {
    static get(parameter, url) {
        if (!url) url = window.location.href;
        parameter = parameter.replace(/[\[\]]/g, "\\$&");
        let regex = new RegExp("[?&]" + parameter + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    }
}