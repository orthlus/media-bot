package art.aelaort.social;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

@Component
public class ProxyCustomizer implements RestTemplateCustomizer {
	@Value("${proxy-url}")
	private URI proxyUrl;

	@Override
	public void customize(RestTemplate restTemplate) {
		String host = proxyUrl.getHost();
		int port = proxyUrl.getPort();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

		requestFactory.setProxy(proxy);
		restTemplate.setRequestFactory(requestFactory);
	}
}
