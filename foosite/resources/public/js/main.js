function grantData() {
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
						x: (point.year- 1970) * 3.15569e7, // year in seconds
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

		var xAxis = new Rickshaw.Graph.Axis.Time({
			graph: graph
		});

		var yAxis = new Rickshaw.Graph.Axis.Y({
			graph: graph
		});

		var hoverDetail = new Rickshaw.Graph.HoverDetail({
			graph: graph,
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
}

function collaborationData() {
	var orgs = [];
	var collaborators = {};
	var $select = $('select');

	function formatData(org) {
		var orig = org.collaborators;
		var matrix = new Array(orig.length + 1);
		var zeroArray = [];
		for (var i = 0, l = orig.length + 1; i < l; i++) {
			zeroArray.push(0);
		}
		for (i = 0, l = orig.length + 1; i < l; i++) {
			matrix[i] = zeroArray.slice(0);
		}

		orig.forEach(function(collaborator, i) {
			matrix[0][i+1] = collaborator.total;
			matrix[i+1][0] = collaborator.total;
		});

		return matrix;
	}

	$.getJSON('/collaborations', function(data) {
		data.forEach(function(el) {
			orgs.push(el.organisation);
			collaborators[el.organisation] = el;
		});

		orgs.forEach(function(org) {
			$('<option/>', { value: org }).text(org)
				.appendTo($select);
		});

		$select.chosen();
	});

	$select.on('change', function(ev) {
		$('svg').remove();

		var org = collaborators[$(this).val()];

		var matrix = formatData(org);

		var chord = d3.layout.chord()
			.padding(.05)
			.sortSubgroups(d3.descending)
			.matrix(matrix);

		var width = $(document).width(),
			height = $(document).height() - 50,
			innerRadius = Math.min(width, height) * .41,
			outerRadius = innerRadius * 1.1;

		var fill = d3.scale.ordinal()
			.domain(d3.range(4))
			.range(["#000000", "#FFDD89", "#957244", "#F26223"]);

		var svg = d3.select('body').append('svg')
			.attr('width', width)
			.attr('height', height)
			.append('g')
				.attr('transform', 'translate(' + width / 2 +
					',' + height/2 + ')');

		svg.append('g').selectAll('path')
			.data(chord.groups)
			.enter().append('path')
				.style('fill', function(d) { return fill(d.index); })
				.style('stroke', function(d) { return fill(d.index); })
				.attr('d', d3.svg.arc().innerRadius(innerRadius)
					.outerRadius(outerRadius))
				.on('mouseover', fade(.1))
				.on('mouseout', fade(1));

		// XXX: ticks go here

		svg.append('g')
			.attr('class', 'chord')
			.selectAll('path')
				.data(chord.chords)
				.enter().append('path')
					.attr('d', d3.svg.chord().radius(innerRadius))
					.style('fill', function(d) { return fill(d.target.index); })
					.style('opacity', .7)
					.style('stroke', '#000')
					.style('stroke-width', '.5px');

		function fade(opacity) {
			return function(g, i) {
				svg.selectAll(".chord path")
					.filter(function(d) {
						return d.source.index != i && d.target.index != i;
					}).transition()
					.style("opacity", opacity);
			};
		}
	});
}

if (/collaborations/.test(window.location.href)) {
	collaborationData();
} else {
	grantData();
}
