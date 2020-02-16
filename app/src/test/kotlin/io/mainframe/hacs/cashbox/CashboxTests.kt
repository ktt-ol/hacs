package io.mainframe.hacs.cashbox

import org.junit.Assert
import org.junit.Test

class CashboxValueTaskTest {

    @Test
    fun parseHtml() {
        val input = """
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title>KTT Shop System: Cashbox</title>
		<link type="text/css" rel="stylesheet" href="/css/bootstrap.css" />
		<link type="text/css" rel="stylesheet" href="/css/bootstrap-theme.css" />
		<link type="text/css" rel="stylesheet" href="/css/dataTables.bootstrap.css" />
		<link type="text/css" rel="stylesheet" href="/css/base.css" />
		<script type="text/javascript" src="/js/jquery.js"></script>
		<script type="text/javascript" src="/js/jquery.mousewheel.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.selection.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.navigate.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.pie.js"></script>
		<script type="text/javascript" src="/js/jquery.dataTables.js"></script>
		<script type="text/javascript" src="/js/bootstrap.js"></script>
		<script type="text/javascript" src="/js/dataTables.bootstrap.js"></script>
		<script type="text/javascript" src="/js/code39.js"></script>
	</head>
	<body>
		<div class="navbar navbar-default navbar-fixed-top">
			<a class="navbar-brand" href="/">KTT Shop System</a>

<ul class="navbar-nav nav">
	<li class=""><a href="/">Home</a></li>
	<li class=" dropdown">
		<a href="#" class="dropdown-toggle" data-toggle="dropdown">Products
			<b class="caret"></b>
		</a>
		<ul class="dropdown-menu">
			<li>
				<a href="/products">List</a>
			</li>
			<li>
				<a href="/products/bestbefore">Best before dates</a>
			</li>
			<li>
				<a href="/products/inventory">Start inventory</a>
			</li>
		</ul>
	</li>
	
	<li class=""><a href="/suppliers">Suppliers</a></li>

	<li class=""><a href="/aliases">Aliases</a></li>
	<li class="active "><a href="/cashbox">Cashbox</a></li>
<!--
	<li class=" dropdown">
		<a href="#" id="statsmenu" class="dropdown-toggle" data-toggle="dropdown">Statistics <b class="caret"></b></a>
		<ul class="dropdown-menu">
			<li><a href="/stats">Information</a></li>
			<li><a href="/stats/stock">Graph: Stock</a></li>
			<li><a href="/stats/profit_per_day">Graph: Profit / Day</a></li>
			<li><a href="/stats/profit_per_weekday">Graph: Profit / Weekday</a></li>
			<li><a href="/stats/profit_per_product">Graph: Profit / Product</a></li>
		</ul>
	</li>
-->
	<li class="  dropdown">
		<a href="#" id="usersmenu" class="dropdown-toggle" data-toggle="dropdown">Users <b class="caret"></b></a>
		<ul class="dropdown-menu">
			<li><a href="/users">List</a></li>
			<li><a href="/users/import">Import</a></li>
			<li><a href="/users/import-pgp">Import PGP Keys</a></li>
		</ul>
	</li>
</ul>

<ul class="navbar-nav nav pull-right">
	<li id="usermenu" class="drop-down">
		<a href="#" id="usermenulink" role="button" class="dropdown-toggle" data-toggle="dropdown"><i class="icon-user"></i>&nbsp;Some User&nbsp;<b class="caret"></b></a>
		<ul class="dropdown-menu" role="menu" aria-labelledby="usermenulink">
			<li role="menuitem"><a tabindex="-1" href="/users/1">Personal Data</a></li>
			<li role="menuitem"><a tabindex="-1" href="/users/1/invoice">Invoice</a></li>
			<li role="menuitem"><a tabindex="-1" href="/users/1/stats">Statistics</a></li>
			<li role="menuitem" class="divider"></li>
			<li role="menuitem"><a tabindex="-1" href="/logout">Logout</a></li>
		</ul>
	</li>
</ul>


		</div>
		<div class="content">
			<legend>Current Cashbox Status</legend>

<div class="input-group">
	<input name"status" class="form-control" type="number" readonly="readonly" value="12.34"/>
	<div class="input-group-addon">€</div>
</div>

<legend>Update Cashbox</legend>
<form action="/cashbox/add" class="form-inline">
	<div class="form-group">
		<select name="type" size="1" class="form-control">
			<option value="loss">Loss (Money is missing from the cashbox)</option>
			<option value="withdrawal">Withdrawal (You removed money from the cashbox)</option>
			<option value="donation">Donation (Cashbox has more money, than there should be)</option>
			<option value="deposit">Deposit (You added money to the cashbox)</option>
		</select>
		<div class="input-group">
			<input name="amount" type="number" placeholder="Amount (in Cent)" class="form-control" />
			<div class="input-group-addon">Cent</div>
		</div>
		<button type="submit" class="form-control btn btn-primary"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button>
	</div>
</form>

<legend>History (last ten updates)</legend>
<table class="table table-bordered table-striped table-hover table-condensed table-nonfluid">
	<tr><td>Date &amp; Time</td><td>Name</td><td>Amount</td></tr>
	<tr><td>Date &amp; Time</td><td>Name</td><td>Amount</td></tr>
	<tr><td>2020-02-11 02:27:14</td><td>Donation</td><td class="text-right">0.20 €</td></tr>
<tr><td>2020-02-10 21:24:37</td><td>Loss</td><td class="text-right">-1.40 €</td></tr>
<tr><td>2020-02-09 19:28:09</td><td>Loss</td><td class="text-right">-0.60 €</td></tr>
<tr><td>2020-02-08 07:21:45</td><td>Foo Bar</td><td class="text-right">-27.90 €</td></tr>
<tr><td>2020-02-08 07:19:45</td><td>Donation</td><td class="text-right">0.20 €</td></tr>
<tr><td>2020-02-06 14:02:42</td><td>Mr X</td><td class="text-right">0.00 €</td></tr>
<tr><td>2020-02-05 21:31:30</td><td>Foo Bar</td><td class="text-right">0.00 €</td></tr>
<tr><td>2020-02-04 19:37:41</td><td>Foo Bar</td><td class="text-right">-5.70 €</td></tr>
<tr><td>2020-02-04 19:37:13</td><td>Loss</td><td class="text-right">-0.40 €</td></tr>
<tr><td>2020-02-02 23:58:33</td><td>Mr X</td><td class="text-right">0.00 €</td></tr>

</table>

<legend>Details</legend>
<div class="form-inline">
	<div class="form-group">
		<input id="yearDetail" class="form-control" type="number" placeholder="Year"/>
		<input id="monthDetail" class="form-control" type="number" placeholder="Month"/>
		<button class="form-control btn btn-default" onclick="showDetails()"></span>Show Details</button>
	</div>
</div>

<script>
function showDetails() {
	var year = document.getElementById("yearDetail").value;
	var month = document.getElementById("monthDetail").value;
	location.href = location.pathname + "/details/" + year + "/" + month;
}
</script>

		</div>
	</body>
</html>

        """.trimIndent()

        val authMock: Auth = Auth("", "", "")
        val result = CashboxValueTask(authMock, { _, _, _ -> }).parseHtml(input)
        Assert.assertEquals("12.34", result.value)
        Assert.assertEquals(10, result.history.size)
        println(result)

    }
}