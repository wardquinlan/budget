$(document).ready(function() {
	$('#j_username').focus();
	$('#j_username').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			$('#j_password').focus();
		}
	});
});