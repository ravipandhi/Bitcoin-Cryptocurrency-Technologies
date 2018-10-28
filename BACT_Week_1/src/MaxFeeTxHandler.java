import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.print.attribute.standard.NumberOfInterveningJobs;

public class MaxFeeTxHandler {
	UTXOPool thisUTXOPool = null;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public MaxFeeTxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		thisUTXOPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are
	 *         valid, (3) no UTXO is claimed multiple times by {@code tx}, (4)
	 *         all of {@code tx}s output values are non-negative, and (5) the
	 *         sum of {@code tx}s input values is greater than or equal to the
	 *         sum of its output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		double sumOfOutputValues = 0;
		double sumOfInputValues = 0;
		if (tx != null)
		{
			// CHECK FOR (1) STARTS  //check in the input whether the transaction that it is referring to is really unspent 

			if (tx.numInputs() > 0)
			{
				for (Transaction.Input input : tx.getInputs())
				{
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					if (!thisUTXOPool.contains(utxo))
						return false;
				}
			}

			// CHECK FOR (1) ENDS

			//check for (2) starts //verify that the current transaction is signed by the owners of the coins in the input (the owners can be fetched from the outputs that is referred by the inputs)
			if (tx.numInputs() > 0)
			{
				for (int i = 0; i < tx.numInputs(); i++)
				{
					UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
					Transaction.Output output = thisUTXOPool.getTxOutput(utxo);
					sumOfInputValues += output.value; //sum of all the input values

					if (Crypto.verifySignature(output.address, tx.getRawDataToSign(i),
							tx.getInput(i).signature) == false)
						return false;
				}

			}
			else
			{
				System.out.println("There are no inputs in the current transaction.");
				return false;
			}
			//check for (2) ends

			//check for (3) starts //verify that there is no double spend by ensuring that the unspent transactions in the pool are only referenced once.
			if (thisUTXOPool.getAllUTXO() != null && thisUTXOPool.getAllUTXO().size() > 0)
			{
				Transaction tr = new Transaction(tx);
				for (UTXO utxo : thisUTXOPool.getAllUTXO())
				{
					tr.removeInput(utxo);
				}
				if (tr.numInputs() > 0)
					return false;
			}
			//check for (3) ends

			//check for (5) starts //verify that no new coins are produced by the transaction 

			//find the sum of output values and compare

			if (tx.numOutputs() > 0)
			{
				for (Transaction.Output output : tx.getOutputs())
				{
					if (output.value >= 0)
						sumOfOutputValues += output.value;
					else
						return false;
				}
			}

			if (sumOfInputValues < sumOfOutputValues)
			{
				return false;
			}
			//check for (5) ends
		}
		else
		{
			System.out.println("Transaction is null");
			return false;
		}

		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		if (possibleTxs.length > 0)
		{
			Transaction[] acceptedTxs = new Transaction[possibleTxs.length];
			int i = 0;
			BigDecimal totalInputValue = null, totalOutputValue = null, maximumTransFees = BigDecimal.valueOf(-1),
					transFees = null;
			Transaction.Output output = null;
			UTXO utxo = null;
			for (Transaction aTx : possibleTxs)
			{
				if (isValidTx(aTx))
				{
					totalInputValue = new BigDecimal(0);
					totalOutputValue = new BigDecimal(0);
					for (int j = 0; j < aTx.numInputs(); j++) //if the transaction is valid, the coins are not spent and so remove those coins from the unspent pool
					{
						utxo = new UTXO(aTx.getInput(j).prevTxHash, aTx.getInput(j).outputIndex);
						output = thisUTXOPool.getTxOutput(utxo);
						totalInputValue.add(BigDecimal.valueOf(output.value));
						thisUTXOPool.removeUTXO(utxo);
					}
					for (int j = 0; j < aTx.numOutputs(); j++)
					{
						utxo = new UTXO(aTx.getHash(), j); //a new output is created which has some new unspent coins and so add those coins to the unspent pool
						output = aTx.getOutput(j);
						totalOutputValue.add(BigDecimal.valueOf(output.value));
						thisUTXOPool.addUTXO(utxo, output);
					}
					transFees = totalInputValue.subtract(totalOutputValue);
					if (transFees.compareTo(maximumTransFees) == 1)
					{
						maximumTransFees = transFees;
						acceptedTxs = new Transaction[possibleTxs.length];
						i = 0;
					}
					else if (transFees.equals(maximumTransFees))
					{
						maximumTransFees = transFees;
						acceptedTxs[i++] = aTx;
					}
				}
			}
			return acceptedTxs;
		}
		else
		{
			return new Transaction[0];
		}
	}

}
