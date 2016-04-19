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
  results.shift();
}

////////////////////////////////////////////////

//Tests for colouring logic
function doColouring(){
  for(var i = 0; i < results.length; i++){
    var symbol = results[i][0];
    var color = results[i][1];
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
        if (color == -50)changeColorRed("#twitter-logo");
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
  var row = table.insertRow(1); //Int at the end is index of Table --> 1 is first line
  //Add logic with a for loop and a counter to automatic fill the table with data

  // Insert new cells (<td> elements) at the 1st and 2nd position of the "new" <tr> element
  var cell1 = row.insertCell(0);
  var cell2 = row.insertCell(1);
  var cell3 = row.insertCell(2);
  var cell4 = row.insertCell(3);

  // Add some text to the new cells
  cell1.innerHTML = "1";
  cell2.innerHTML = results[0][0];
  cell3.innerHTML = "Add logic here!";
  cell4.innerHTML = results[0][1];
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
