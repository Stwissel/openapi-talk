// Once DOM is loaded initialize SwaggerUI
if (document.readyState != 'loading') {
  bootstrapSwagger();
} else {
  document.addEventListener('DOMContentLoaded', bootstrapSwagger);
}

function bootstrapSwagger() {
  const defaultURL = '/openapidemo.json';

  window.ui = SwaggerUIBundle({
    dom_id: '#swagger-ui',
    docExpansion: 'full',
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    displayOperationId: true,
    displayRequestDuration: true,
    defaultModelsExpandDepth: 10,
    defaultModelExpandDepth: 10,
    defaultExpanded: true,
    showExtensions: true,
    showCommonExtensions: true,
    syntaxHighlight: { activate: true, theme: 'monokai' },
    tryItOutEnabled: true,
    requestSnippetsEnabled: true,
    onComplete: completed,
    plugins: [SwaggerUIBundle.plugins.DownloadUrl],
    layout: 'StandaloneLayout',
    queryConfigEnabled: true,
    validatorUrl: null,
    url: defaultURL
  });
}

// Get protocol, port, server
function completed() {
  // Link back to home on first link, the logo
  let firstLink = document.querySelector('a');
  if (firstLink) {
    firstLink.href = '/';
  }
  console.log('OpenAPI initialized');
}
