$(document).ready(function() {
	$('#accounts').dataTable({
		"bSort": false,
		"pageLength": 25
	});
	$('#accountName').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
});