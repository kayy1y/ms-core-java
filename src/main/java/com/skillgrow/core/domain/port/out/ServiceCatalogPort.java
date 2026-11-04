package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.ServiceSnapshot;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Datos que MS1 lee de MS2 (dueño de los servicios y del catálogo de habilidades). Es una de las dos
 * únicas llamadas síncronas entre microservicios. Si MS2 no responde → ServiceUnavailableException.
 */
public interface ServiceCatalogPort {

    /** Servicio existente y no eliminado; empty si no existe. */
    Optional<ServiceSnapshot> findService(String serviceId);

    /** Nombre de cada habilidad del catálogo; las que no existen no aparecen en el mapa. */
    Map<String, String> skillNames(Collection<String> skillIds);
}
