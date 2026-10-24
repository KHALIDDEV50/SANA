package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Model.ProjectOutput;

// Legacy/domain writers must not bypass Architecture's revision-checked endpoints.
public final class ArchitectureWriteGuard {
    private ArchitectureWriteGuard() {}

    public static void rejectUnversionedWrite(ProjectOutput output) {
        if (output != null && TechnicalOutputType.ARCHITECTURE.name().equals(output.getOutputType())) {
            throw new ApiException("Use the Architecture endpoints with expectedRevision to modify this diagram");
        }
    }
}
