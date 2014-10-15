package edu.hawaii.senin.postgre.db;

import java.io.IOException;

/**
 * Manages DB instances preventing multiple objects instantiation.
 * 
 * @author psenin
 * 
 */
public class PostgreDBManager {

  private static PostgreDB productionInstance;

  /**
   * Get production DB instance.
   * 
   * @return production DB instance.
   * @throws IOException if error occurs.
   */
  public static PostgreDB getProductionInstance() throws IOException {
    if (null == productionInstance) {
      productionInstance = new PostgreDB(PostgreDB.PRODUCTION_INSTANCE);
    }
    return productionInstance;
  }

}
