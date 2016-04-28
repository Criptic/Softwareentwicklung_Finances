var avg50days;
var avg250days;

var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 960 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var formatDate = d3.time.format("%Y-%m-%d");

var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");

var line = d3.svg.line()
    .x(function(d) { return x(d.Date); })
    .y(function(d) { return y(d.Close); });

var svg = d3.select("#apple-graph").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

d3.csv("../main/resources/AAPL.csv", type, function(error, data) {
  if (error) throw error;

  x.domain(d3.extent(data, function(d) { return d.Date; }));
  y.domain(d3.extent(data, function(d) { return d.Close; }));

  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);

  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis)
    .append("text")
      //.attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Price $");

  svg.append("path")
      .datum(data)
      .attr("class", "line")
      .attr("id", "mainline")
      .attr("d", line);
});

d3.csv("../main/resources/AAPLIncludingAvg.csv", function(data){
avg50days = data[0]["Avg50Days"];
avg250days = data[0]["Avg250Days"];

  svg.append("line")
      .attr('x1', '0')
      .attr('x2', width)
      .attr('y1', y(avg50days))
      .attr('y2', y(avg50days))
      .attr('id', 'avg50days');

  svg.append("line")
      .attr('x1', '0')
      .attr('x2', width)
      .attr('y1', y(avg250days))
      .attr('y2', y(avg250days))
      .attr('id', 'avg250days');
});

function type(d) {
  d.Date = formatDate.parse(d.Date);
  d.Close = +d.Close;
  return d;
}
