//Defining Variables for global use
var results = [];


//jQuery pre processor for handling a file in browser on client side
$.ajaxPrefilter( 'script', function( options ) {
    options.crossDomain = true;
});

//Get Data from definded url. In this case it is a path
$.ajax({
    type: "GET",
    url: "../main/resources/Result.csv",
    dataType: "text",
    success: function(data) {
        console.log(data);
        //console.log($.type(data));
        //Convert string to an array
        parseString(data);
        //Logic for colouring
        doColouring();
        //Build the Table
        buildTable();
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
  //Delete first Element in Array because it is the header of csv
  results.shift();
}

////////////////////////////////////////////////

//Tests for colouring logic
function doColouring(){
  for(var i = 0; i < results.length; i++){
    var symbol = results[i][0];
    var color = results[i][5];
    switch(symbol) {
      case "AAPL":
        if (color < 0)changeColorRed("#apple-logo");
        if (color == 0)changeColorYellow("#apple-logo");
        if (color > 0)changeColorGreen("#apple-logo");
        break;
      case "AMZN":
        if (color < 0)changeColorRed("#amazon-logo");
        if (color == 0)changeColorYellow("#amazon-logo");
        if (color > 0)changeColorGreen("#amazon-logo");
        break;
      case "FB":
        if (color < 0)changeColorRed("#facebook-logo");
        if (color == 0)changeColorYellow("#facebook-logo");
        if (color > 0)changeColorGreen("#facebook-logo");
        break;
      case "GOOG":
        if (color < 0)changeColorRed("#google-logo");
        if (color == 0)changeColorYellow("#google-logo");
        if (color > 0)changeColorGreen("#google-logo");
        break;
      case "MSFT":
        if (color < 0)changeColorRed("#microsoft-logo");
        if (color == 0)changeColorYellow("#microsoft-logo");
        if (color > 0)changeColorGreen("#microsoft-logo");
        break;
      case "TWTR":
        if (color < 0)changeColorRed("#twitter-logo");
        if (color == 0)changeColorYellow("#twitter-logo");
        if (color > 0)changeColorGreen("#twitter-logo");
        break;
    }
    //console.log(symbol, color);
  }
}

////////////////////////////////////////////////

//Add Values to Table
function buildTable(){
  //Get Table from DOM Element
  var table = document.getElementById("table");

  // Create an empty <tr> element and add it to the 1st position of the table
  var row1 = table.insertRow(1); //Int at the end is index of Table --> 1 is first line
  var row2 = table.insertRow(2);
  var row3 = table.insertRow(3);
  var row4 = table.insertRow(4);
  var row5 = table.insertRow(5);
  var row6 = table.insertRow(6);

  // Insert new cells (<td> elements) at the "new" <tr> element
  var cell11 = row1.insertCell(0);
  var cell12 = row1.insertCell(1);
  var cell13 = row1.insertCell(2);
  var cell14 = row1.insertCell(3);

  var cell21 = row2.insertCell(0);
  var cell22 = row2.insertCell(1);
  var cell23 = row2.insertCell(2);
  var cell24 = row2.insertCell(3);

  var cell31 = row3.insertCell(0);
  var cell32 = row3.insertCell(1);
  var cell33 = row3.insertCell(2);
  var cell34 = row3.insertCell(3);

  var cell41 = row4.insertCell(0);
  var cell42 = row4.insertCell(1);
  var cell43 = row4.insertCell(2);
  var cell44 = row4.insertCell(3);

  var cell51 = row5.insertCell(0);
  var cell52 = row5.insertCell(1);
  var cell53 = row5.insertCell(2);
  var cell54 = row5.insertCell(3);

  var cell61 = row6.insertCell(0);
  var cell62 = row6.insertCell(1);
  var cell63 = row6.insertCell(2);
  var cell64 = row6.insertCell(3);

  //Add Ranking Numbers
  cell11.innerHTML = "1";
  cell21.innerHTML = "2";
  cell31.innerHTML = "3";
  cell41.innerHTML = "4";
  cell51.innerHTML = "5";
  cell61.innerHTML = "6";

  //Add Symbols
  cell12.innerHTML = results[0][0];
  cell22.innerHTML = results[1][0];
  cell32.innerHTML = results[2][0];
  cell42.innerHTML = results[3][0];
  cell52.innerHTML = results[4][0];
  cell62.innerHTML = results[5][0];

  //Add Scores
  cell14.innerHTML = results[0][5].substring(0,6);
  cell24.innerHTML = results[1][5].substring(0,6);
  cell34.innerHTML = results[2][5].substring(0,6);
  cell44.innerHTML = results[3][5].substring(0,6);
  cell54.innerHTML = results[4][5].substring(0,6);
  cell64.innerHTML = results[5][5].substring(0,6);

  //Dummy
  cell13.innerHTML = "Add logic here!";
}







////////////////////////////////////////////////

//Dummy function to change Color of Thumbnail
function changeColorRed(id){
  d3.select(id).style("background-color", "#F44336");
}

function changeColorYellow(id){
  d3.select(id).style("background-color", "#FFC107");
}


function changeColorGreen(id){
  d3.select(id).style("background-color", "#388E3C");
}
