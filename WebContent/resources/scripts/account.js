$(document).ready(function() {
	$('#transactions').dataTable({
		"bSort": false,
		"pageLength": 10
	});
	$('#transactionAmount').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
	$('#name').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
	$('#transactionAmount').focus();
});