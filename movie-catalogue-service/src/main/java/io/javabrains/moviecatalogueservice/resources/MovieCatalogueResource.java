package io.javabrains.moviecatalogueservice.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.javabrains.moviecatalogueservice.models.CatalogueItem;
import io.javabrains.moviecatalogueservice.models.Movie;
import io.javabrains.moviecatalogueservice.models.UserRating;

@RestController
@RequestMapping("/catalogue")
public class MovieCatalogueResource {
	
	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private RestTemplate restTemplate;
	
	// DiscoveryClient is a custom method for service discovery, where you want to perform load balance yourself, you may call getInstances method to get the instances,
	// this is not recommended
	@Autowired
	private DiscoveryClient discoveryClient;

	@RequestMapping("/{userId}")
	public List<CatalogueItem> getCatalogue(String userId) {

		// Get All the movie's that user has rated.
		// This will directly call the microservice without asking for the microservice from the service discovery,
		// @LoadBalanced is not needed on your restTemplate/webClient, if you provide @LoadBalanced to your rest template/webclient 
		// with this kind of URL it will not find the instance of microservice, and exception will  be thrown,
		// this url calling is like your are simply calling the microservice directly. -> not recommended
		// UserRating userRating = restTemplate.getForObject("http://localhost:8083/ratingsdata/users/" + userId, UserRating.class);
		
		// This will first ask for the microservice from the service discovery, and then call the microservice.
		// For this to work @LoadBalanced annotation is necessary on your restTemplate/webClient, @LoadBalanced will first ask for the microservice 
		// from the service discovery and then call the microservice in a load balanced way if there are more than one instance of that microservice.
		UserRating userRating = restTemplate.getForObject("http://RATINGS-DATA-SERVICE/ratingsdata/users/" + userId, UserRating.class);
		
		// For each movie ID, call movie info service and get movie details.
		return userRating.getUserRating().stream().map(rating -> {
			
			// using RestTemplate
			Movie movie = restTemplate.getForObject("http://movie-info-service/movies/" + rating.getMovieId(), Movie.class);
			
			// using WebClient - WebClient is kind of asynchronous
//			Movie movie = webClientBuilder.build()
//							.get()
//							.uri("http://localhost:8082/movies/" + rating.getMovieId())
//							.retrieve()
//							.bodyToMono(Movie.class)
//							.block(); // block blocks the execution till we get that result from the asynchronous BodytoMono.
			
			return new CatalogueItem(movie.getName(), "Test", rating.getRating());
		}).collect(Collectors.toList());// Put them all together

		// ######################################################### Notes #########################################################
		// For service discovery we will be using eureka server.
		// For creating eureka server we will have to create a separate spring boot project, here we have created the project with the
		// name discovery-server.
		// This project will have a dependency called spring-cloud-starter-netflix-eureka-server
		// Rest all the 3 projects will have eureka client dependency.
		// All the 3 projects will register themselves on the eureka server as eureka clients, and tell the eureka server that if 
		// anyone asks for a me, tell them i am here.
		// Also out of these 3 projects, movie catalogue project will also look other 2 micro services from the eureka server. The eureka server
		// will respond with the service details and then the movie catalogue directly calls that service. This is called client side
		// service discovery.
		
		// Spring Cloud uses Client side service discovery. 
		// Server side discovery is also possible, but not with spring cloud i think.
		// Server side discovery is : Client tells the eureka server to look for a microservice, and call that microservices for me.
		
		// Eureka is one of the projects provided by Netflix OSS for service discovery. It is open source. Other projects provided by Netflix oss
		// are Ribbon, Zuul and Hysterix, all of them are used for service discovery. All of them are open source.
		
		// Netflix is the leaders in microservices libraries made open source that work well with spring boot. 
		
		// These 4 technologies are compatible with Spring.
		
		// Spring cloud is a technology for building microservices applications
		// All those netflix oss techonologies mentioned above(eureka, ribbon etc) are wrappers of spring cloud, which incorporates(implements netflix projects)
		
		
		// To make a Eureka server, add the eureka server dependency and the annotation @EnableEurekaServer in the spring boot application class. This will
		// start eureka server. The default port of eureka server is 8761, but since it also a spring boot project it messes with the spring boot tomcat default
		// port. To avoid such mess, do explicitly declare the eureka server port in application.properties file.
		
		// Also when Eureka server starts it also registers itself as a client so you need to disable that too, because you want to get register as a 
		// eureka server and not eureka client. 
		
		// Eureka server tries to registers itself as a client (if not disabled) because there can be more than two eureka servers.
		
		// To make a Eureka Client, add the eureka client dependency should be added and this will register the application as a microservice
		// on the eureka server, provided that the eureka server is running, and provided that it is running on its default port. Otherwise you need to specify
		// the port of eureka server in the application.properties file in the client applications.
		// @EnableEurekaClient annotation is optional. It is just a marker annotation, with no significance.
		
		// Hardcoded URL's are bad.
		// Because when microservices are deployed, you will never know what the URL of the microservice would be on the cloud, hence hardcoding urls is bad, also 
		// when there are more than 1 instance of any microservice, there will be more than one urls so which url will you hardcode. Also when there  is more than 1 
		// instance of a microservice you will need loadbalancing.
		
		// There is one dependency of repository in  client projects in pom, if something doesnt work look kaushiks lectutre number 20.
		
		// To see load-balancing in action, run one more instance of movie info service from the command line, by using the below command, after running the below
		// command two instances of movie-info-service would be visible on eureka server localhost:8761
		
		// java -Dserver.port=8201 -jar movie-info-service-0.0.1-SNAPSHOT.jar
		
		// Apart from this to manage faults, the eureka clients need to send heartbeats to eureka server informing the eureka server that i am still alive. 
		
	}
}

