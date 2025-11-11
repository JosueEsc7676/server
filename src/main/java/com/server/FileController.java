package com.server;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileController {

    // Ruta base dentro de resources/static
    private static final String UPLOAD_DIR = "static/uploads";

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/android")
    public String android(Model model) throws IOException {
        File folder = new ClassPathResource(UPLOAD_DIR + "/android").getFile();
        List<String> archivos = obtenerArchivos(folder);

        boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("APP"));

        String nombreVisible = "MiApp Android";
        String nombrePrograma = tienePartes ? "APP.part" : (archivos.isEmpty() ? "" : archivos.get(0));

        model.addAttribute("tipo", "android");
        model.addAttribute("nombreVisible", nombreVisible);
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", !archivos.isEmpty());

        String descripcion = "Aplicación Android institucional - Permite gestionar procesos desde dispositivos móviles.";
        model.addAttribute("descripcion", descripcion);

        String imagen = "/img/android.png";
        model.addAttribute("imagen", imagen);

        return "android";
    }

    @GetMapping("/escritorio")
    public String escritorio(Model model) throws IOException {
        File folder = new ClassPathResource(UPLOAD_DIR + "/escritorio").getFile();
        List<String> archivos = obtenerArchivos(folder);

        boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("GDOC"));
        String nombrePrograma = tienePartes ? "GDOC.part" : (archivos.isEmpty() ? "" : archivos.get(0));

        model.addAttribute("tipo", "escritorio");
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", !archivos.isEmpty());

        String descripcion = "Sistema de Gestión de Incapacidad de Docentes - Permite administrar incapacidades y generar reportes institucionales.";
        model.addAttribute("descripcion", descripcion);

        String imagen = "/img/programa.png";
        model.addAttribute("imagen", imagen);

        return "escritorio";
    }

    private List<String> obtenerArchivos(File folder) {
        if (!folder.exists() || !folder.isDirectory()) return List.of();
        return Arrays.stream(folder.listFiles())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    @GetMapping("/descargar")
    public ResponseEntity<FileSystemResource> descargar(
            @RequestParam String tipo,
            @RequestParam String nombre) throws IOException {

        File folder = new ClassPathResource(UPLOAD_DIR + "/" + tipo).getFile();

        File[] partes = folder.listFiles((dir, fname) -> fname.startsWith(nombre));

        if (partes != null && partes.length > 0) {
            Arrays.sort(partes);

            String extension = tipo.equals("android") ? "" : ".exe";

            File archivoFinal = File.createTempFile(nombre, extension);
            try (FileOutputStream fos = new FileOutputStream(archivoFinal)) {
                for (File parte : partes) {
                    java.nio.file.Files.copy(parte.toPath(), fos);
                }
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + extension + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(archivoFinal));
        }

        File archivo = new File(folder, nombre);
        if (!archivo.exists()) {
            return ResponseEntity.notFound().build();
        }

        String extension = tipo.equals("android") ? ".apk" : ".exe";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + extension + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(archivo));
    }
}
