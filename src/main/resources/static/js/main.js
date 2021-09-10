
let isTimeoutRunning = false;

let resultObject = {
    formSubmitionResult: null
}

$(document).on('pagebeforeshow', '#login', function () {
    $.mobile.defaultPageTransition = "slide";

    $(document).on('click', '#submit', function () { // catch the form's submit event
        const username = $('#username').val().toLowerCase();
        const password = $('#password').val();

        if (username.length > 0 && password.length > 0) {

            jQuery.support.cors = true;

            $.ajax( {   url: '/api/auth/login?username=' + username + "&password=" + password,
                        type: 'POST',
                        async: true,
                        contentType: 'application/json; charset=UTF-8',
                        dataType: 'text',

                        beforeSend: function () {
                            // This callback function will trigger before data is sent
                            $.mobile.showPageLoadingMsg(true); // This will show ajax spinner
                        },
                        complete: function () {
                            // This callback function will trigger on data sent/received complete
                            $.mobile.hidePageLoadingMsg(); // This will hide ajax spinner
                        },
                        success: function (result) {
                            resultObject.formSubmitionResult = result;
                            $.mobile.changePage("#24s");
                        },
                        error: function () {
                            alert('Probleem met login');
                        }
                    });
        } else {
            alert('Gelieve gebruikersnaam en paswoord in te geven.');
        }
        return false; // cancel original event to prevent form submitting
    });
});

$(document).on('pagebeforeshow', '#24s', function () {
    if (resultObject.formSubmitionResult == null) {
        $.mobile.changePage("#login");
    }
});

function execute(url) {
    jQuery.support.cors = true;

    $.ajax( {   url: url + "?token=" + resultObject.formSubmitionResult,
        type: 'put',
        dataType: 'json',

        beforeSend: function () {
            // This callback function will trigger before data is sent
            $.mobile.showPageLoadingMsg(true); // This will show ajax spinner
        },
        complete: function () {
            // This callback function will trigger on data sent/received complete
            $.mobile.hidePageLoadingMsg(); // This will hide ajax spinner
        }
    });
}

$(document).on("click", "#startbtn", function() {

    jQuery.support.cors = true;
    $("#plusone").hide();
    $("#minusone").hide();
    $("#sixty").hide();

    execute("/api/twentyfour/start");
});

$(document).on("click", "#stopbtn", function() {

    jQuery.support.cors = true;

    if (isTimeoutRunning) {
        isTimeoutRunning = false;
        execute("/api/timeout/stop");
    } else {
        $("#plusone").show();
        $("#minusone").show();
        $("#sixty").show();
        $("#startbtn").show();

        execute("/api/twentyfour/stop");
    }
});

$(document).on("click", "#timeout", function() {
    isTimeoutRunning = true;

    jQuery.support.cors = true;

    execute("/api/timeout/start");
});

$(document).on("click", "#fourteen", function() {
    execute("/api/twentyfour/fourteen");
});

$(document).on("click", "#twentyfour", function() {
    execute("/api/twentyfour/reset");
});

$(document).on("click", "#plusone", function() {
    execute("/api/twentyfour/inc");
});

$(document).on("click", "#minusone", function() {
    execute("/api/twentyfour/dec");
});

