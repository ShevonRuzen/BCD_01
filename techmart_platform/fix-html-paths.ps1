$base = Join-Path $PWD 'src\main\webapp'
Get-ChildItem -Path $base -Recurse -Filter *.html | ForEach-Object {
    $path = $_.FullName
    $text = Get-Content -Raw $path
    $orig = $text
    $isAdmin = $path -match '\\admin\\'
    $isUser = $path -match '\\user\\'

    if ($isAdmin) {
        $text = $text -replace 'href="/styles.css"', 'href="../styles.css"'
        $text = $text -replace 'src="/common.js"', 'src="../common.js"'
        $text = $text -replace 'href="/logout.html"', 'href="../logout.html"'
        $text = $text -replace 'href="/login.html"', 'href="../login.html"'
        $text = $text -replace 'href="/register.html"', 'href="../register.html"'
        $text = $text -replace "requireLogin\('/login.html'\)", "requireLogin('../login.html')"
        $text = $text -replace 'window.location.href = "/login.html"', 'window.location.href = "../login.html"'
        $text = $text -replace 'window.location.href = "/admin/([^"]+)"', 'window.location.href = "$1"'
        $text = $text -replace 'window.location.href = "/user/([^"]+)"', 'window.location.href = "../user/$1"'
        $text = $text -replace 'href="/admin/([^"]+)"', 'href="$1"'
        $text = $text -replace 'href="/user/([^"]+)"', 'href="../user/$1"'
    }
    elseif ($isUser) {
        $text = $text -replace 'href="/styles.css"', 'href="../styles.css"'
        $text = $text -replace 'src="/common.js"', 'src="../common.js"'
        $text = $text -replace 'href="/logout.html"', 'href="../logout.html"'
        $text = $text -replace 'href="/login.html"', 'href="../login.html"'
        $text = $text -replace 'href="/register.html"', 'href="../register.html"'
        $text = $text -replace "requireLogin\('/login.html'\)", "requireLogin('../login.html')"
        $text = $text -replace 'window.location.href = "/login.html"', 'window.location.href = "../login.html"'
        $text = $text -replace 'window.location.href = "/admin/([^"]+)"', 'window.location.href = "../admin/$1"'
        $text = $text -replace 'window.location.href = "/user/([^"]+)"', 'window.location.href = "$1"'
        $text = $text -replace 'href="/admin/([^"]+)"', 'href="../admin/$1"'
        $text = $text -replace 'href="/user/([^"]+)"', 'href="$1"'
    }
    else {
        $text = $text -replace 'href="/styles.css"', 'href="styles.css"'
        $text = $text -replace 'src="/common.js"', 'src="common.js"'
        $text = $text -replace 'href="/login.html"', 'href="login.html"'
        $text = $text -replace 'href="/register.html"', 'href="register.html"'
        $text = $text -replace 'href="/logout.html"', 'href="logout.html"'
        $text = $text -replace 'window.location.href = "/login.html"', 'window.location.href = "login.html"'
        $text = $text -replace 'window.location.href = "/register.html"', 'window.location.href = "register.html"'
        $text = $text -replace 'window.location.href = "/admin/([^"]+)"', 'window.location.href = "admin/$1"'
        $text = $text -replace 'window.location.href = "/user/([^"]+)"', 'window.location.href = "user/$1"'
        $text = $text -replace 'href="/admin/([^"]+)"', 'href="admin/$1"'
        $text = $text -replace 'href="/user/([^"]+)"', 'href="user/$1"'
    }

    if ($text -ne $orig) {
        Set-Content -Path $path -Value $text
        Write-Host "Updated $path"
    }
}