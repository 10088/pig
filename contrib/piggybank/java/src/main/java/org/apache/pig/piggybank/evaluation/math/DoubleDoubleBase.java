/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.piggybank.evaluation.math;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.FuncSpec;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.WrappedIOException;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.data.DataType;

/**
* base class for math udfs that return Double value
*/
public abstract class DoubleDoubleBase extends Base{
    // each derived class provides the computation here
    abstract Double compute(Double input1, Double input2);

	/**
	 * java level API
	 * @param input expects a tuple with a single Double value
	 * @param output returns a Double value
     */
	public Double exec(Tuple input) throws IOException {
        if (input == null || input.size() < 2)
            return null;

        try {
            Double val1 = (Double)input.get(0);
            Double val2 = (Double)input.get(1);
            return (val1 == null || val2 == null) ? null : compute(val1, val2);
        } catch (Exception e){
             throw WrappedIOException.wrap("Caught exception processing input of " + this.getClass().getName(), e);
        }
	}

    /* (non-Javadoc)
     * @see org.apache.pig.EvalFunc#getArgToFuncMapping()
     */
    @Override
    public List<FuncSpec> getArgToFuncMapping() throws FrontendException {
        List<FuncSpec> funcList = new ArrayList<FuncSpec>();
        List<Schema.FieldSchema> fields = new ArrayList<Schema.FieldSchema>();
        fields.add(new Schema.FieldSchema(null, DataType.DOUBLE));
        fields.add(new Schema.FieldSchema(null, DataType.DOUBLE));
        funcList.add(new FuncSpec(this.getClass().getName(), new Schema(fields)));

        return funcList;
    }

}
