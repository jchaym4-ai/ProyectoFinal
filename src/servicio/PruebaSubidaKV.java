
package servicio;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class PruebaSubidaKV {

    public static void main(String[] args) {

        try {

            Properties config = new Properties();

            Path archivoConfig =
                    Path.of("cloudflare.properties");

            if (!Files.exists(archivoConfig)) {
                throw new IllegalStateException(
                        "No se encontró cloudflare.properties."
                );
            }

            try (InputStream entrada =
                    Files.newInputStream(archivoConfig)) {

                config.load(entrada);
            }

            String token = config.getProperty(
                    "cloudflare.api.token"
            );

            String accountId = config.getProperty(
                    "cloudflare.account.id"
            );

            String namespaceId = config.getProperty(
                    "cloudflare.namespace.id"
            );

            if (token == null || token.isBlank()
                    || accountId == null || accountId.isBlank()
                    || namespaceId == null || namespaceId.isBlank()) {

                throw new IllegalStateException(
                        "La configuración de Cloudflare está incompleta."
                );
            }

            Path archivo = Path.of(
                    System.getProperty("user.home"),
                    "SVB-GUA",
                    "boletos",
                    "prueba.pdf"
            );

            if (!Files.isRegularFile(archivo)) {
                throw new IllegalStateException(
                        "No existe el PDF de prueba: " + archivo
                );
            }

            String url =
                    "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId
                    + "/storage/kv/namespaces/"
                    + namespaceId
                    + "/values/pdf%3Aprueba";

            HttpRequest solicitud =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(30))
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
                                            .ofFile(archivo)
                            )
                            .build();

            try (HttpClient cliente =
                    HttpClient.newHttpClient()) {

                HttpResponse<String> respuesta =
                        cliente.send(
                                solicitud,
                                HttpResponse.BodyHandlers.ofString()
                        );

                System.out.println(
                        "HTTP: " + respuesta.statusCode()
                );

                if (respuesta.statusCode() < 200
                        || respuesta.statusCode() >= 300) {

                    System.out.println(respuesta.body());

                    throw new IllegalStateException(
                            "Cloudflare rechazó la subida."
                    );
                }

                System.out.println(
                        "PDF subido correctamente a Workers KV."
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
