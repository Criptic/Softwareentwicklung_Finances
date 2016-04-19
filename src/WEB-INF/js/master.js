//Defining Variables for global use
var results = [];


//jQuery pre processor for handling a file in browser on client side
$.ajaxPrefilter( 'script', function( options ) {
    options.crossDomain = true;
});

//Get Data from definded url. In this case it is a path
$.ajax({
    type: "GET",
    url: "../main/resources/dummy.csv",
    dataType: "text",
    success: function(data) {
        console.log(data);
        console.log($.type(data));
        parseString(data);
    },
    error: function (request, status, error) {

        alert(error);
    }
 });

//Papaparse function for casting csv string into array.
//Function is called in success case as callback function
function parseString(string){
  results = Papa.parse(string);
  //show the data in console as array
  console.log(results.data);
  //Give value to the global variable
  results = results.data;
}



////////////////////////////////////////////////

//Dummy function to change Color of Thumbnail
function changeColorRed(){
  d3.select("#apple-logo").style("background-color", "#F44336");
}

function changeColorYellow(){
  d3.select("#twitter-logo").style("background-color", "#FFC107");
}


function changeColorGreen(){
  d3.select("#facebook-logo").style("background-color", "#388E3C");
}
