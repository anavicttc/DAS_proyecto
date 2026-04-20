<?php
$DB_SERVER = "db";
$DB_USER = "admin";
$DB_PASS = "test";
$DB_DATABASE = "database";

$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);
if (mysqli_connect_errno()) {
    echo json_encode(array("status" => "error", "mensaje" => "error de conexion"));
    exit();
}

$usuario = $_POST["usuario"];
$contrasena = $_POST["contrasena"];

$sql = "SELECT * FROM usuarios WHERE nombre = '$usuario' AND contrasena = '$contrasena'";
$resultado = mysqli_query($con, $sql);

// SOLUCIÓN 1: mysqli_fetch_assoc para poder usar el nombre de la columna 'id'
if ($fila = mysqli_fetch_assoc($resultado)){ 
    // SOLUCIÓN 2: Guardar en la variable en vez de hacer echo directamente
    $respuesta = array("status" => "ok", "mensaje" => "bienvenidoo", "id_usuario" => $fila['id']); 
} else {
    //usuario no encontrado o contraseña mal
    $respuesta = array("status" => "error", "mensaje" => "usuario o contraseña incorrectos");
}

echo json_encode($respuesta);
?>
