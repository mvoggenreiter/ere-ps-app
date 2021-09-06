package health.ere.ps.resource.pdf;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import health.ere.ps.model.gematik.BundleWithAccessCodeOrThrowable;
import health.ere.ps.service.pdf.DocumentService;

@Path("/document")
public class DocumentResource {
    @Inject
    DocumentService documentService;

    IParser parser = FhirContext.forR4().newJsonParser();

    @POST
    @Path("/bundles")
    public Response createAndSendPrescriptions(String bundlesString) {

        JsonArray jsonArray = Json.createReader(new StringReader(bundlesString)).readArray();

        List<BundleWithAccessCodeOrThrowable> bundles = jsonArray.stream().map(jv -> {
            return convert(jv);
        }).filter(Objects::nonNull).collect(Collectors.toList());

        ByteArrayOutputStream boas = documentService.generateERezeptPdf(bundles);
        return Response.ok().entity(boas.toByteArray()).type("application/pdf").build();
    }

    private BundleWithAccessCodeOrThrowable convert(JsonValue jv) {
        BundleWithAccessCodeOrThrowable bt = new BundleWithAccessCodeOrThrowable();
        if(jv instanceof JsonObject) {
            JsonObject jo = (JsonObject) jv;
            bt.setAccessCode(jo.getString("accessCode"));
            bt.setBundle(parser.parseResource(Bundle.class, jo.getJsonObject("bundle").toString()));
        }
        return bt;
    }
}
