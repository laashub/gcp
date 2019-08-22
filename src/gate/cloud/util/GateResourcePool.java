/*
 *  GateResourcePool.java
 *  Copyright (c) 2007-2011, The University of Sheffield.
 *
 *  This file is part of GCP (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Affero General Public License,
 *  Version 3, November 2007.
 *
 *
 *  $Id: GateResourcePool.java 18567 2015-02-10 13:20:37Z johann_p $ 
 */
package gate.cloud.util;

import gate.Factory;
import gate.Resource;
import gate.creole.*;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

/**
 * Class representing a pool of independent but equivalent GATE
 * Resources.
 */
public class GateResourcePool<T extends Resource>  implements Iterable<T> {
  private static int uniqueNumber = 1;
  
  private static final Logger log = Logger.getLogger(GateResourcePool.class);

  /**
   * The pool.
   */
  private BlockingQueue<T> pool;

  /**
   * Take a controller from the pool. This method will block if no
   * controllers are available. The controller returned by this method
   * <i>must</i> be returned to the pool by calling {@link #release},
   * typically in a try/finally construct.
   */
  public T take() throws InterruptedException {
    return pool.take();
  }

  /**
   * Return to the pool a controller that was taken with {@link #take}.
   */
  public void release(T c) {
    pool.add(c);
  }
  
  public void dispose() {
    for(T res : pool) {
      Factory.deleteResource(res);
    }
    pool.clear();
  }

  /**
   * Fill the pool by taking the given controller as a prototype and
   * creating a number of independent copies to add to the pool. The
   * template controller will be one of the controllers in the resulting
   * pool, i.e. if the pool size is 2 then one extra copy will be
   * created from the prototype and that copy plus the original template
   * will be added to the pool.
   */
  public void fillPool(T templateResource, int poolSize)
          throws ResourceInstantiationException {
    log.debug("Filling pool with " + poolSize + " copies of template resource");
    if(poolSize == 0) {
      pool = new ArrayBlockingQueue<T>(1);
      return;
    }
    // can use an ArrayBlockingQueue as we'll never add more than
    // poolSize items to the queue
    pool = new ArrayBlockingQueue<T>(poolSize);

    log.debug("Using template resource as one member of pool");
    this.release(templateResource);
    poolSize--;

    while(poolSize > 0) {
      log.debug("Creating independent copy of resource");
      this.release((T)Factory.duplicate(templateResource));
      poolSize--;
    }
  }
  
  @Override
  public Iterator<T> iterator() {
    return pool.iterator();
  }

}
