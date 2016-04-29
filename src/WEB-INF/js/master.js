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

        alert("An error has occured. Are you using chrome? Please use the safari browser for a better visulization of the webpage");
    }
 });





////////////////////////////////////////////////

//FUNCTIONS


//Function for colouring logic
function doColouring(){
  for(var i = 0; i < results.length; i++){
    var symbol = results[i][0];
    var color = results[i][5];
    switch(symbol) {
      case "AAPL":
        if (color < 0)changeColor("#apple-logo", "#F44336");
        if (color == 0)changeColor("#apple-logo", "#FFC107");
        if (color > 0)changeColor("#apple-logo", "#388E3C");
        break;
      case "AMZN":
        if (color < 0)changeColor("#amazon-logo", "#F44336");
        if (color == 0)changeColor("#amazon-logo", "#FFC107");
        if (color > 0)changeColor("#amazon-logo", "#388E3C");
        break;
      case "FB":
        if (color < 0)changeColor("#facebook-logo", "#F44336");
        if (color == 0)changeColor("#facebook-logo", "#FFC107");
        if (color > 0)changeColor("#facebook-logo", "#388E3C");
        break;
      case "GOOG":
        if (color < 0)changeColor("#google-logo", "#F44336");
        if (color == 0)changeColor("#google-logo", "#FFC107");
        if (color > 0)changeColor("#google-logo", "#388E3C");
        break;
      case "MSFT":
        if (color < 0)changeColor("#microsoft-logo", "#F44336");
        if (color == 0)changeColor("#microsoft-logo", "#FFC107");
        if (color > 0)changeColor("#microsoft-logo", "#388E3C");
        break;
      case "TWTR":
        if (color < 0)changeColor("#twitter-logo", "#F44336");
        if (color == 0)changeColor("#twitter-logo", "#FFC107");
        if (color > 0)changeColor("#twitter-logo", "#388E3C");
        break;
    }
    //console.log(symbol, color);
  }
}

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

  //Add ranking numbers
  cell11.innerHTML = "1";
  cell21.innerHTML = "2";
  cell31.innerHTML = "3";
  cell41.innerHTML = "4";
  cell51.innerHTML = "5";
  cell61.innerHTML = "6";

  //Add symbols
  cell12.innerHTML = results[0][0];
  cell22.innerHTML = results[1][0];
  cell32.innerHTML = results[2][0];
  cell42.innerHTML = results[3][0];
  cell52.innerHTML = results[4][0];
  cell62.innerHTML = results[5][0];

  //Add company name
  cell13.innerHTML = getFullName(results[0][0]);
  cell23.innerHTML = getFullName(results[1][0]);
  cell33.innerHTML = getFullName(results[2][0]);
  cell43.innerHTML = getFullName(results[3][0]);
  cell53.innerHTML = getFullName(results[4][0]);
  cell63.innerHTML = getFullName(results[5][0]);

  //Add scores
  cell14.innerHTML = results[0][5].substring(0,6);
  cell24.innerHTML = results[1][5].substring(0,6);
  cell34.innerHTML = results[2][5].substring(0,6);
  cell44.innerHTML = results[3][5].substring(0,6);
  cell54.innerHTML = results[4][5].substring(0,6);
  cell64.innerHTML = results[5][5].substring(0,6);

}

//Function to change color of thumbnails
function changeColor(id, colorCode) {
  d3.select(id).style("background-color", colorCode);
}

//Function to translate the stock symbol into the full name of the company
function getFullName(stockSymbol) {
  switch (stockSymbol) {
    case "AMZN":
      return "Amazon";
    case "AAPL":
      return "Apple";
    case "FB":
      return "Facebook";
    case "GOOG":
      return "Google";
    case "MSFT":
      return "Microsoft";
    case "TWTR":
      return "Twitter";
  }
}
