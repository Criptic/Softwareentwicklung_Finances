//Javascript for visulization with d3.js
//More infos: https://d3js.org

//Defining variables for global usage
var path;
var avgpath;
var avg50days;
var avg250days;

//Get the pathname and split it to the name of the html file
var page = window.location.pathname;
page = page.split("/").pop();

//Switch statement for setting the path of the csv and the csv with average values
switch (page) {
  case "apple.html":
    path = "../main/resources/AAPL.csv";
    avgpath = "../main/resources/AAPLIncludingAvg.csv";
    break;
  case "amazon.html":
    path = "../main/resources/AMZN.csv";
    avgpath = "../main/resources/AMZNIncludingAvg.csv";
    break;
  case "facebook.html":
    path = "../main/resources/FB.csv";
    avgpath = "../main/resources/FBIncludingAvg.csv";
    break;
  case "google.html":
    path = "../main/resources/GOOG.csv";
    avgpath = "../main/resources/GOOGIncludingAvg.csv";
    break;
  case "microsoft.html":
    path = "../main/resources/MSFT.csv";
    avgpath = "../main/resources/MSFTIncludingAvg.csv";
    break;
  case "twitter.html":
    path = "../main/resources/TWTR.csv";
    avgpath = "../main/resources/TWTRIncludingAvg.csv";
    break;
  default:

}

var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 960 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

//Date format of the date in the csv for displaying it in the axis
var formatDate = d3.time.format("%Y-%m-%d");

//Scales for d3.js
var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

//X and Y axis and teir orientation
var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");

//Defining where to find the values in the csv
var line = d3.svg.line()
    .x(function(d) { return x(d.Date); })
    .y(function(d) { return y(d.Close); });

//Defining the svg and where to add it
var svg = d3.select("#graph").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

//Parse the csv in the path variable for displaying the graph
d3.csv(path, type, function(error, data) {
  if (error) throw error;

  x.domain(d3.extent(data, function(d) { return d.Date; }));
  y.domain(d3.extent(data, function(d) { return d.Close; }));

  //Setting the x axis
  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);

  //setting the y axis
  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis)
    .append("text")
      //.attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Price $");

  //Draw the graph
  svg.append("path")
      .datum(data)
      .attr("class", "line")
      .attr("id", "mainline")
      .attr("d", line);
});

//Pars the csv file with the average values for the stock symbol.
d3.csv(avgpath, function(data){
avg50days = data[0]["Avg50Days"];
avg250days = data[0]["Avg250Days"];

  //Draw the line for avg50
  svg.append("line")
      .attr('x1', '0')
      .attr('x2', width)
      .attr('y1', y(avg50days))
      .attr('y2', y(avg50days))
      .attr('id', 'avg50days');

  //Draw the line for avg250
  svg.append("line")
      .attr('x1', '0')
      .attr('x2', width)
      .attr('y1', y(avg250days))
      .attr('y2', y(avg250days))
      .attr('id', 'avg250days');

  //Write the two values in the legend
  document.getElementById('text-avg50days').innerHTML+=(avg50days.substring(0,6));
  document.getElementById('text-avg250days').innerHTML+=(avg250days.substring(0,6));

});

//Date formatting
function type(d) {
  d.Date = formatDate.parse(d.Date);
  d.Close = +d.Close;
  return d;
}
