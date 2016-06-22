import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sivagurunathan.v on 15/06/16.
 */
public class D42Application extends Application<AppConfiguration> {
  public static final Logger logger = LoggerFactory.getLogger(D42Application.class);

  public static void main(String[] args) throws Exception {
    new D42Application().run(args);
  }

  @Override
  public void initialize(Bootstrap<AppConfiguration> bootstrap) {
    super.initialize(bootstrap);
  }
  @Override
  public void run(AppConfiguration configuration, Environment environment) throws Exception {
    D42Service d42Service = new D42Service();
    D42Util d42Util = new D42Util();
    logger.info("started");
    environment.jersey().register(d42Service);
    environment.jersey().register(d42Util);
  }
}
