var _allcards=TAFFY();
var filter="";

$(document).ready(function () {
	$.getJSON("/api/data/cards", function (data) {
		console.log(data.count);
		_allcards=TAFFY(data.data)
		write_allcards()
	});
	
	function write_allcards()	{
		$('#cardlist').empty()
		$('#resultsummary').html(_allcards(filter).count() + '/' + _allcards().count() + ' cards returned');
		_allcards(filter).order("code").each(function (c) {
			$('#cardlist').append(newline(c));
		});
	}
	
	function newline(c) {
		var outp =''
		outp = '<tr>'
			+ '<td>' + c.code + '</td>'
			+ '<td>' + c.name + '</td>'
			+ '<td>' + c.type_name + '</td>'
			+ '<td>' + c.pack_name + '</td>'
			+ '<td>' + c.position + '</td>'
			+ '<td><span style="white-space: pre-wrap;">' + c.text + '</span></td>'
			+ '</tr>';
		return outp;
	}
	
	$('#filter').on('change', function() {
		filter = {};
		if ($(this).val() !== "") {
			filter = JSON.parse($(this).val());
		}	
		write_allcards();
	});
});