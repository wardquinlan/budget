$(document).ready(function() {
	$('#currentPassword').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
	$('#newPassword').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
	$('#newPassword2').keydown(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});
	
	$('#currentPassword').focus();
});