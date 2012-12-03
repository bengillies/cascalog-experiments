$.getJSON('/organisations', function(data) {
	var palette = new Rickshaw.Color.Palette();

	var series = data.map(function(data) {
		return {
			name: data.organisation,
			color: palette.color(),
			data: data.totals.map(function(point) {
				return {
					x: point.year,
					y: point.total
				};
			})
		};
	});

	Rickshaw.Series.zeroFill(series, 0)

	var graph = new Rickshaw.Graph({
		element: $('#graph')[0],
		renderer: 'line',
		series: series
	});

	var legend = new Rickshaw.Graph.Legend({
		graph: graph,
		element: $('#legend')[0]
	});

	var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
		graph: graph,
		legend: legend
	});

	var order = new Rickshaw.Graph.Behavior.Series.Order({
		graph: graph,
		legend: legend
	});

	var highlighter = new Rickshaw.Graph.Behavior.Series.Highlight({
		graph: graph,
		legend: legend
	});

	var time = new Rickshaw.Fixtures.Time()
	years = time.unit('year');
	var xAxis = new Rickshaw.Graph.Axis.Time({
		graph: graph,
		timeUnit: years
	});

	var yAxis = new Rickshaw.Graph.Axis.Y({
		graph: graph
	});

	var hoverDetail = new Rickshaw.Graph.HoverDetail({
		graph: graph,
		xFormatter: function(x) { return x; },
		yFormatter: function(y) { return '£' + y; }
	});

	graph.render();
	xAxis.render();
	yAxis.render();


//	nv.addGraph(function() {
//		var chart = nv.models.lineChart();
//		chart.xAxis.axisLabel("Year").tickFormat(d3.format(',r'));
//		chart.yAxis.axisLabel("Grant Total").tickFormat(d3.format(',r'));
//
//		//var chart = nv.models.discreteBarChart()
//		//	.x(function(d) { return d.organisation; })
//		//	.y(function(d) { return d.total; })
//		//	.staggerLabels(true)
//		//	.tooltips(false)
//		//	.showValues(true);
//
//		d3.select('svg')
//			.attr('width', 1000)
//			.attr('height', 700)
//			.datum(data.slice(0,10))
//			.transition().duration(500)
//				.call(chart);
//	});
});
