(function() {
	var graph, store;

	var limits = {
		min: 1,
		max: 0,
		limit: 0
	};

	function setData(data) {
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

		Rickshaw.Series.zeroFill(series, 0);

		return series;
	}

	$.getJSON('/organisations', function(data) {
		limits.max = data.length;
		limits.limit = data.length;
		store = data;

		var series = setData(data);

		graph = new Rickshaw.Graph({
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

		domb.bind(limits, $('#num-orgs')[0], 'limit');

		$('#num-orgs').on('mousedown', function(ev) {
			var xStart = ev.pageX;

			var redraw = fp.throttle(updateGraph, 100);

			function onMove(ev) {
				var newX = ev.pageX;
					diff = newX - xStart;

				diff = diff < limits.min ? limits.min : (diff > limits.max ?
					limits.max : diff);

				limits.limit = diff;

				redraw();
			}

			function updateGraph() {
				graph.series.forEach(function(s, i) {
					if (i >= limits.limit) {
						s.disabled = true;
					} else {
						s.disabled = false;
					}
				});
				graph.update();
			}

			$(document).on('mousemove', onMove);
			$(document).on('mouseup', function() {
				$(document).off('mousemove', onMove);
			});
		});
	});
}());
