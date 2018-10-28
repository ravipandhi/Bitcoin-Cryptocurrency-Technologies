import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.print.attribute.standard.NumberOfInterveningJobs;

public class TxHandler {
	UTXOPool thisUTXOPool = null;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
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
			// CHECK FOR (1) STARTS  //check in the input transaction whether the transaction that it is referring to it really unspent 

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

			//check for (2) starts
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

			//check for (3) starts
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

			//check for (5) starts

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
			for (Transaction aTx : possibleTxs)
			{
				if (isValidTx(aTx))
				{
					acceptedTxs[i++] = aTx;
					for (int j = 0; j < aTx.numInputs(); j++)
					{
						UTXO utxo = new UTXO(aTx.getInput(j).prevTxHash, aTx.getInput(j).outputIndex);
						thisUTXOPool.removeUTXO(utxo);
					}
					for (int j = 0; j < aTx.numOutputs(); j++)
					{
						UTXO utxo = new UTXO(aTx.getHash(), j);
						thisUTXOPool.addUTXO(utxo, aTx.getOutput(j));
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
