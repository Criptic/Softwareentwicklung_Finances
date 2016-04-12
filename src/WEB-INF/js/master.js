function showApple(){
  alert('clicked');
}

//Dummy load Data from papaparse
function loadFile(){
  Papa.parse("../../src/main/resources/dummy.csv", {
 	download: true,
 	complete: function(results) {
 		console.log(results);
 	}
 });
}

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
