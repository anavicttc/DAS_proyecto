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

//comprobamos si existe
$sql_comprobar = "SELECT * FROM usuarios WHERE nombre = '$usuario'";
$resultado_comprobar = mysqli_query($con, $sql_comprobar);

//si rdo>=1 el usuario ya existe
if (mysqli_num_rows($resultado_comprobar) > 0) {
    echo json_encode(array("status" => "error", "mensaje" => "nombre de usuario en uso, elige otro"));
    exit();
}
//insertamos
$sql_insertar = "INSERT INTO usuarios (nombre, contrasena, foto_path) VALUES ('$usuario', '$contrasena', '')";
$resultado_insertar = mysqli_query($con, $sql_insertar);

if ($resultado_insertar) {
    $id_nuevo_usuario = mysqli_insert_id($con);
    echo json_encode(array(
        "status" => "ok",
        "mensaje" => "usuario registrado correctamente",
        "id_usuario" => $id_nuevo_usuario
    ));
} else {
    echo json_encode(array("status" => "error", "mensaje" => mysqli_error($con)));
}
?>
