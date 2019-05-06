package co.b4pay.api.controller.home;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author YK
 * @version $Id v 0.1 2017年08月08日 15:31 Exp $
 */
@RestController
@RequestMapping("/")
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

//    @Autowired
//    private JpaRepository<SecretKey, String> jpaRepository;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet() {
//        long count = jpaRepository.count();
//        if (logger.isInfoEnabled()) {
//            logger.info("Summary Count:::{}", count);
//        }
        return "hello !";
    }
}
