rootProject.name = "dobrovichek"

include(
    "libs:common-contracts",
    "libs:common-events",
    "libs:common-security",
    "libs:jwt-support",
    "services:api-gateway",
    "services:identity-service",
    "services:user-service",
    "services:request-service",
    "services:notification-service",
)
