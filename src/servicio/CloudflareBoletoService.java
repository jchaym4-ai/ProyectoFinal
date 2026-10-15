
package servicio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;

public class CloudflareBoletoService {

    private static final String URL_PUBLICA =
            "https://svb-gua-boletos.jchaym4.workers.dev";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private static final int MAX_PDF_BYTES =
            25 * 1024 * 1024;

    private final String accountId;
    private final String namespaceId;
    private final String token;

    public CloudflareBoletoService() throws IOException {

        Properties config = new Properties();

        Path archivoConfig =
                Path.of("cloudflare.properties");

        if (!Files.isRegularFile(archivoConfig)) {
            throw new IOException(
                    "No se encontró cloudflare.properties."
            );
        }

        try (InputStream entrada =
                Files.newInputStream(archivoConfig)) {

            config.load(entrada);
        }

        accountId = obtenerPropiedad(
                config,
                "cloudflare.account.id"
        );

        namespaceId = obtenerPropiedad(
                config,
                "cloudflare.namespace.id"
        );

        token = obtenerPropiedad(
                config,
                "cloudflare.api.token"
        );
    }

    private String obtenerPropiedad(
            Properties config,
            String nombre) throws IOException {

        String valor = config.getProperty(nombre);

        if (valor == null || valor.isBlank()) {
            throw new IOException(
                    "Falta la configuración: " + nombre
            );
        }

        return valor.trim();
    }

    public String generarIdentificador() {

        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    public String construirUrl(String identificador) {

        validarIdentificador(identificador);

        return URL_PUBLICA + "/b/" + identificador;
    }

    public String subirPDF(
            Path archivoPDF,
            String identificador)
            throws IOException, InterruptedException {

        validarIdentificador(identificador);

        if (archivoPDF == null
                || !Files.isRegularFile(archivoPDF)) {

            throw new IOException(
                    "No existe el PDF que se desea subir."
            );
        }

        long tamano = Files.size(archivoPDF);

        if (tamano == 0 || tamano > MAX_PDF_BYTES) {
            throw new IOException(
                    "El archivo PDF está vacío "
                    + "o supera los 25 MiB."
            );
        }

        try (InputStream entrada =
                Files.newInputStream(archivoPDF)) {

            byte[] firma = entrada.readNBytes(5);

            if (!java.util.Arrays.equals(
                    firma,
                    "%PDF-".getBytes(StandardCharsets.US_ASCII))) {

                throw new IOException(
                        "El archivo no tiene una cabecera PDF válida."
                );
            }
        }

        String clave = "pdf:" + identificador;

        String claveCodificada = URLEncoder.encode(
                clave,
                StandardCharsets.UTF_8
        );

        String endpoint =
                "https://api.cloudflare.com/client/v4/accounts/"
                + accountId
                + "/storage/kv/namespaces/"
                + namespaceId
                + "/values/"
                + claveCodificada;

        HttpRequest solicitud =
                HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(60))
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .header(
                                "Content-Type",
                                "application/pdf"
                        )
                        .PUT(
                                HttpRequest.BodyPublishers
                                        .ofFile(archivoPDF)
                        )
                        .build();

        try (HttpClient cliente =
                HttpClient.newHttpClient()) {

            HttpResponse<String> respuesta =
                    cliente.send(
                            solicitud,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (respuesta.statusCode() < 200
                    || respuesta.statusCode() >= 300
                    || !respuesta.body().contains(
                            "\"success\":true"
                    )
                    && !respuesta.body().matches(
                            "(?s).*\"success\"\\s*:\\s*true.*"
                    )) {

                throw new IOException(
                        "No se pudo publicar el PDF. HTTP "
                        + respuesta.statusCode()
                );
            }
        }

        return construirUrl(identificador);
    }

    private void validarIdentificador(String identificador) {

        if (identificador == null
                || !identificador.matches(
                        "[a-f0-9]{32}"
                )) {

            throw new IllegalArgumentException(
                    "Identificador de boleto inválido."
            );
        }
    }
}
