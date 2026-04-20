<?php
$DB_SERVER = "db";
$DB_USER = "admin";
$DB_PASS = "test";
$DB_DATABASE = "database";
$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);

if (mysqli_connect_errno()) {
    echo json_encode(array("status" => "error", "mensaje" => "error de conexión"));
    exit();
}

$nombre = $_POST['nombre'];
$contrasena = $_POST['contrasena']; // Recibimos como 'contrasena'
$base64_image = $_POST['imagen'];
$id_usuario = $_POST['id_usuario']; 

$nombre_fichero = "";

if (!empty($base64_image)) {
    $binary = base64_decode($base64_image);
    $nombre_fichero = "perfil_" . $id_usuario . ".jpg";
    $file = fopen($nombre_fichero, 'w+');
    fwrite($file, $binary);
    fclose($file);
}

if ($nombre_fichero != "") {
    $sql = "UPDATE usuarios SET nombre = ?, contrasena = ?, foto_path = ? WHERE id = ?";
    $stmt = mysqli_prepare($con, $sql);
    mysqli_stmt_bind_param($stmt, "sssi", $nombre, $contrasena, $nombre_fichero, $id_usuario);
} else {
    $sql = "UPDATE usuarios SET nombre = ?, contrasena = ? WHERE id = ?";
    $stmt = mysqli_prepare($con, $sql);
    // SOLUCIÓN: Cambiado $password por $contrasena
    mysqli_stmt_bind_param($stmt, "ssi", $nombre, $contrasena, $id_usuario); 
}

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(array("status" => "ok", "mensaje" => "perfil act"));
} else {
    echo json_encode(array("status" => "error", "mensaje" => mysqli_error($con)));
}

mysqli_close($con);
?>
