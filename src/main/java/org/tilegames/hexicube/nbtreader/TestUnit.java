package org.tilegames.hexicube.nbtreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestUnit
{
	public static void main(String[] args) throws IllegalArgumentException, IOException
	{
		System.out.println("Unit test activated, press enter within 10 seconds to begin!");
		BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
		long start = System.nanoTime();
		while(true)
		{
			if(sysIn.ready()) break;
			try{Thread.sleep(150);}catch(InterruptedException e){}
			if(System.nanoTime()-start > 10000000000L)
			{
				System.out.println("Took too long, assuming no console view! Exiting!");
				return;
			}
		}
		sysIn.readLine();
		System.out.println("Running preliminary test! (expect a GZIP warning)");
		System.out.println("Please enter the location of test.nbt");
		NBT data = NBT.fromFile(new File(sysIn.readLine()));
		System.out.println("Data parsed ok, checking...");
		if(!data.mainTag.name.equals("hello world"))
		{
			System.err.println("Test failed, main tag not called \"hello world\"!");
			return;
		}
		if(data.mainTag.tagList.size() == 0)
		{
			System.err.println("Test failed, main tag empty!");
			return;
		}
		if(data.mainTag.tagList.size() > 1)
		{
			System.err.println("Test failed, main tag holds multiple tags when it should only hold one!");
			return;
		}
		NBTTag t = data.mainTag.tagList.get(0);
		if(!(t instanceof NBTString))
		{
			System.err.println("Test failed, sub-tag not a string!");
			return;
		}
		NBTString s = (NBTString)t;
		if(!s.name.equals("name"))
		{
			System.err.println("Test failed, sub-tag not called \"name\"!");
			return;
		}
		if(!s.val.equals("Bananrama"))
		{
			System.err.println("Test failed, sub-tag doesn't hold the value \"Bananrama\"!");
			return;
		}
		System.out.println("Preliminary test passed!");
		System.out.println();
		System.out.println("Running GZIP and advanced test!");
		System.out.println("Please enter the location of bigtest.nbt");
		data = NBT.fromFile(new File(sysIn.readLine()));
		System.out.println("Data unzipped and parsed ok, checking...");
		if(!data.mainTag.name.equals("Level"))
		{
			System.err.println("Test failed, main tag not called \"Level\"!");
			return;
		}
		if(data.mainTag.tagList.size() != 11)
		{
			System.err.println("Test failed, main tag did not contain exactly 11 entries!");
			return;
		}
		boolean[] valid = new boolean[11];
		for(int a = 0; a < 11; a++) valid[a] = false;
		for(int a = 0; a < data.mainTag.tagList.size(); a++)
		{
			t = data.mainTag.tagList.get(a);
			if(t instanceof NBTByte)
			{
				if(valid[0])
				{
					System.err.println("Test failed, multiple byte tags exist!");
					return;
				}
				NBTByte b = (NBTByte)t;
				if(!b.name.equals("byteTest"))
				{
					System.err.println("Test failed, byte tag not called \"byteTest\"!");
					return;
				}
				if(b.val != 127)
				{
					System.err.println("Test failed, byte tag not equal to 127!");
					return;
				}
				valid[0] = true;
				System.out.println("Byte tag pass!");
			}
			else if(t instanceof NBTShort)
			{
				if(valid[1])
				{
					System.err.println("Test failed, multiple short tags exist!");
					return;
				}
				NBTShort b = (NBTShort)t;
				if(!b.name.equals("shortTest"))
				{
					System.err.println("Test failed, short tag not called \"shortTest\"!");
					return;
				}
				if(b.val != 32767)
				{
					System.err.println("Test failed, short tag not equal to 32767!");
					return;
				}
				valid[1] = true;
				System.out.println("Short tag pass!");
			}
			else if(t instanceof NBTInt)
			{
				if(valid[2])
				{
					System.err.println("Test failed, multiple int tags exist!");
					return;
				}
				NBTInt b = (NBTInt)t;
				if(!b.name.equals("intTest"))
				{
					System.err.println("Test failed, int tag not called \"intTest\"!");
					return;
				}
				if(b.val != 2147483647)
				{
					System.err.println("Test failed, int tag not equal to 2147483647!");
					return;
				}
				valid[2] = true;
				System.out.println("Int tag pass!");
			}
			else if(t instanceof NBTLong)
			{
				if(valid[3])
				{
					System.err.println("Test failed, multiple long tags exist!");
					return;
				}
				NBTLong b = (NBTLong)t;
				if(!b.name.equals("longTest"))
				{
					System.err.println("Test failed, long tag not called \"longTest\"!");
					return;
				}
				if(b.val != 9223372036854775807L)
				{
					System.err.println("Test failed, long tag not equal to 9223372036854775807!");
					return;
				}
				valid[3] = true;
				System.out.println("Long tag pass!");
			}
			else if(t instanceof NBTFloat)
			{
				if(valid[4])
				{
					System.err.println("Test failed, multiple float tags exist!");
					return;
				}
				NBTFloat b = (NBTFloat)t;
				if(!b.name.equals("floatTest"))
				{
					System.err.println("Test failed, float tag not called \"floatTest\"!");
					return;
				}
				if(b.val != 0.49823147F)
				{
					System.err.println("Test failed, float tag not equal to 0.49823147!");
					return;
				}
				valid[4] = true;
				System.out.println("Float tag pass!");
			}
			else if(t instanceof NBTDouble)
			{
				if(valid[5])
				{
					System.err.println("Test failed, multiple double tags exist!");
					return;
				}
				NBTDouble b = (NBTDouble)t;
				if(!b.name.equals("doubleTest"))
				{
					System.err.println("Test failed, double tag not called \"doubleTest\"!");
					return;
				}
				if(b.val != 0.4931287132182315)
				{
					System.err.println("Test failed, double tag not equal to 0.4931287132182315!");
					return;
				}
				valid[5] = true;
				System.out.println("Double tag pass!");
			}
			else if(t instanceof NBTString)
			{
				if(valid[6])
				{
					System.err.println("Test failed, multiple string tags exist!");
					return;
				}
				NBTString b = (NBTString)t;
				if(!b.name.equals("stringTest"))
				{
					System.err.println("Test failed, string tag not called \"stringTest\"!");
					return;
				}
				if(!b.val.equals("HELLO WORLD THIS IS A TEST STRING ÅÄÖ!"))
				{
					System.err.println("Test failed, string tag not equal to \"HELLO WORLD THIS IS A TEST STRING ÅÄÖ!\"!");
					System.err.println(b.val);
					return;
				}
				valid[6] = true;
				System.out.println("String tag pass!");
			}
			else if(t instanceof NBTCompound)
			{
				if(valid[7])
				{
					System.err.println("Test failed, multiple compound tags exist!");
					return;
				}
				NBTCompound b = (NBTCompound)t;
				if(!b.name.equals("nested compound test"))
				{
					System.err.println("Test failed, compound tag not called \"nested compound test\"!");
					return;
				}
				if(b.tagList.size() != 2)
				{
					System.err.println("Test failed, compound tag didn't have exactly 2 sub-tags!");
					return;
				}
				NBTTag tag1 = b.tagList.get(0);
				NBTTag tag2 = b.tagList.get(1);
				if(!(tag1 instanceof NBTCompound) || !(tag2 instanceof NBTCompound))
				{
					System.err.println("Test failed, compound tag sub-tags were not also compound!");
					return;
				}
				NBTCompound c1 = (NBTCompound)tag1;
				NBTCompound c2;
				if(c1.name.equals("egg"))
				{
					c2 = c1;
					c1 = (NBTCompound)tag2;
					System.err.println("WARNING: Sub-compound tags not in order, ignoring!");
				}
				else c2 = (NBTCompound)tag2;
				if(!c1.name.equals("ham"))
				{
					System.err.println("Test failed, inner sub-tag 1 not called \"ham\"!");
					return;
				}
				if(c1.tagList.size() != 2)
				{
					System.err.println("Test failed, inner sub-tag 1 did not have 2 sub-tags!");
					return;
				}
				boolean[] innerPasses = new boolean[2];
				innerPasses[0] = false;
				innerPasses[1] = false;
				for(int z = 0; z < 2; z++)
				{
					NBTTag inner = c1.tagList.get(z);
					if(inner instanceof NBTString)
					{
						if(innerPasses[0])
						{
							System.err.println("Test failed, inner sub-tag 1 has 2 string sub-tags!");
							return;
						}
						NBTString i = (NBTString)inner;
						if(!i.name.equals("name"))
						{
							System.err.println("Test failed, inner sub-tag 1 string tag not called \"name\"!");
							return;
						}
						if(!i.val.equals("Hampus"))
						{
							System.err.println("Test failed, inner sub-tag 1 string tag not equal to \"Hampus\"!");
							return;
						}
						innerPasses[0] = true;
					}
					else if(inner instanceof NBTFloat)
					{
						if(innerPasses[1])
						{
							System.err.println("Test failed, inner sub-tag 1 has 2 float sub-tags!");
							return;
						}
						NBTFloat i = (NBTFloat)inner;
						if(!i.name.equals("value"))
						{
							System.err.println("Test failed, inner sub-tag 1 float tag not called \"value\"!");
							return;
						}
						if(i.val != 0.75F)
						{
							System.err.println("Test failed, inner sub-tag 1 float tag not equal to 0.75!");
							return;
						}
						innerPasses[1] = true;
					}
					else
					{
						System.err.println("Test failed, inner sub-tag 1 had an incorrect sub-tag type: "+inner.getClass().getName());
						return;
					}
				}
				if(!innerPasses[0] || !innerPasses[1])
				{
					System.err.println("Test failed, inner sub-tag 1 didn't get both passes! Report this!");
					return;
				}
				if(!c2.name.equals("egg"))
				{
					System.err.println("Test failed, inner sub-tag 2 not called \"egg\"!");
					return;
				}
				if(c2.tagList.size() != 2)
				{
					System.err.println("Test failed, inner sub-tag 2 did not have 2 sub-tags!");
					return;
				}
				innerPasses[0] = false;
				innerPasses[1] = false;
				for(int z = 0; z < 2; z++)
				{
					NBTTag inner = c2.tagList.get(z);
					if(inner instanceof NBTString)
					{
						if(innerPasses[0])
						{
							System.err.println("Test failed, inner sub-tag 2 has 2 string sub-tags!");
							return;
						}
						NBTString i = (NBTString)inner;
						if(!i.name.equals("name"))
						{
							System.err.println("Test failed, inner sub-tag 2 string tag not called \"name\"!");
							return;
						}
						if(!i.val.equals("Eggbert"))
						{
							System.err.println("Test failed, inner sub-tag 2 string tag not equal to \"Eggbert\"!");
							return;
						}
						innerPasses[0] = true;
					}
					else if(inner instanceof NBTFloat)
					{
						if(innerPasses[1])
						{
							System.err.println("Test failed, inner sub-tag 2 has 2 float sub-tags!");
							return;
						}
						NBTFloat i = (NBTFloat)inner;
						if(!i.name.equals("value"))
						{
							System.err.println("Test failed, inner sub-tag 2 float tag not called \"value\"!");
							return;
						}
						if(i.val != 0.5F)
						{
							System.err.println("Test failed, inner sub-tag 2 float tag not equal to 0.5!");
							return;
						}
						innerPasses[1] = true;
					}
					else
					{
						System.err.println("Test failed, inner sub-tag 2 had an incorrect sub-tag type: "+inner.getClass().getName());
						return;
					}
				}
				if(!innerPasses[0] || !innerPasses[1])
				{
					System.err.println("Test failed, inner sub-tag 2 didn't get both passes! Report this!");
					return;
				}
				valid[7] = true;
				System.out.println("Nested compound tag pass!");
			}
			else if(t instanceof NBTList)
			{
				NBTList b = (NBTList)t;
				if(b.name.equals("listTest (long)"))
				{
					if(valid[8])
					{
						System.err.println("Test failed, multiple list tags called \"listTest (long)\"!");
						return;
					}
					if(b.tagList.size() != 5)
					{
						System.err.println("Test failed, long list tag did not have 5 sub-tags!");
						return;
					}
					for(int z = 0; z < 5; z++)
					{
						NBTTag l = b.tagList.get(z);
						if(!(l instanceof NBTLong))
						{
							System.err.println("Test failed, long list tag item "+(z+1)+" not a long tag!");
							return;
						}
						if(((NBTLong)l).val != 11+z)
						{
							System.err.println("Test failed, long list tag item "+(z+1)+" not equal to "+(z+11)+"!");
							return;
						}
					}
					valid[8] = true;
					System.out.println("Long list tag pass!");
				}
				else if(b.name.equals("listTest (compound)"))
				{
					if(valid[9])
					{
						System.err.println("Test failed, multiple list tags called \"listTest (compound)\"!");
						return;
					}
					if(b.tagList.size() != 2)
					{
						System.err.println("Test failed, compound list tag did not have 2 sub-tags!");
						return;
					}
					NBTTag tag1 = b.tagList.get(0);
					if(!(tag1 instanceof NBTCompound))
					{
						System.err.println("Test failed, compound list tag item 1 not a compound tag!");
						return;
					}
					NBTCompound c = (NBTCompound)tag1;
					if(c.tagList.size() != 2)
					{
						System.err.println("Test failed, compound list tag item 1 did not have 2 sub-tags!");
						return;
					}
					boolean[] subPasses = new boolean[2];
					subPasses[0] = false;
					subPasses[1] = false;
					for(int z = 0; z < 2; z++)
					{
						NBTTag r = c.tagList.get(z);
						if(r instanceof NBTString)
						{
							if(subPasses[0])
							{
								System.err.println("Test failed, compound list tag item 1 has 2 string sub-tags!");
								return;
							}
							NBTString l = (NBTString)r;
							if(!l.name.equals("name"))
							{
								System.err.println("Test failed, compound list tag item 1 string tag not called \"name\"!");
								return;
							}
							if(!l.val.equals("Compound tag #0"))
							{
								System.err.println("Test failed, compound list tag item 1 string tag does not equal \"Compound tag #0\"!");
								return;
							}
							subPasses[0] = true;
						}
						else if(r instanceof NBTLong)
						{
							if(subPasses[1])
							{
								System.err.println("Test failed, compound list tag item 1 has 2 long sub-tags!");
								return;
							}
							NBTLong l = (NBTLong)r;
							if(!l.name.equals("created-on"))
							{
								System.err.println("Test failed, compound list tag item 1 long tag not called \"created-on\"!");
								return;
							}
							if(l.val != 1264099775885L)
							{
								System.err.println("Test failed, compound list tag item 1 long tag does not equal 1264099775885!");
								return;
							}
							subPasses[1] = true;
						}
						else
						{
							System.err.println("Test failed, compound list tag item 1 sub-item "+(z+1)+" had an incorrect sub-tag type: "+c.getClass().getName());
							return;
						}
					}
					NBTTag tag2 = b.tagList.get(1);
					if(!(tag2 instanceof NBTCompound))
					{
						System.err.println("Test failed, compound list tag item 2 not a compound tag!");
						return;
					}
					c = (NBTCompound)tag2;
					if(c.tagList.size() != 2)
					{
						System.err.println("Test failed, compound list tag item 2 did not have 2 sub-tags!");
						return;
					}
					subPasses[0] = false;
					subPasses[1] = false;
					for(int z = 0; z < 2; z++)
					{
						NBTTag r = c.tagList.get(z);
						if(r instanceof NBTString)
						{
							if(subPasses[0])
							{
								System.err.println("Test failed, compound list tag item 2 has 2 string sub-tags!");
								return;
							}
							NBTString l = (NBTString)r;
							if(!l.name.equals("name"))
							{
								System.err.println("Test failed, compound list tag item 2 string tag not called \"name\"!");
								return;
							}
							if(!l.val.equals("Compound tag #1"))
							{
								System.err.println("Test failed, compound list tag item 2 string tag does not equal \"Compound tag #1\"!");
								return;
							}
							subPasses[0] = true;
						}
						else if(r instanceof NBTLong)
						{
							if(subPasses[1])
							{
								System.err.println("Test failed, compound list tag item 2 has 2 long sub-tags!");
								return;
							}
							NBTLong l = (NBTLong)r;
							if(!l.name.equals("created-on"))
							{
								System.err.println("Test failed, compound list tag item 2 long tag not called \"created-on\"!");
								return;
							}
							if(l.val != 1264099775885L)
							{
								System.err.println("Test failed, compound list tag item 2 long tag does not equal 1264099775885!");
								return;
							}
							subPasses[1] = true;
						}
						else
						{
							System.err.println("Test failed, compound list tag item 2 sub-item "+(z+1)+" had an incorrect sub-tag type: "+c.getClass().getName());
							return;
						}
					}
					valid[9] = true;
					System.out.println("Compound list tag pass!");
				}
				else
				{
					System.err.println("Test failed, list tag not called \"listTest (long)\" or \"listTest (compound)\"!");
					return;
				}
			}
			else if(t instanceof NBTByteArray)
			{
				NBTByteArray b = (NBTByteArray)t;
				if(!b.name.equals("byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))"))
				{
					System.err.println("Test failed, byte array tag not called \"byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))\"!");
					return;
				}
				if(b.data.length != 1000)
				{
					System.err.println("Test failed, byte array tag did not have 1000 bytes!");
					return;
				}
				for(int z = 0; z < 1000; z++)
				{
					int valToTest = (z*z*255+z*7)%100;
					if(valToTest != b.data[z])
					{
						System.err.println("Test failed, byte array tag did not have 1000 bytes!");
						return;
					}
				}
				valid[10] = true;
				System.out.println("Byte array tag pass!");
			}
			else
			{
				System.err.println("Test failed, newer tag type shouldn't be in this data: "+t.getClass().getName());
				return;
			}
		}
		for(int a = 0; a < 11; a++)
		{
			if(!valid[a])
			{
				System.err.println("Test failed, didn't get all 11 passes! Report this!");
				return;
			}
		}
		System.out.println("GZIP and advanced test passed!");
		System.out.println();
		System.out.println("Running file save tests!");
		System.out.println("Please enter a file name and location for the NBT data");
		data.toFile(new File(sysIn.readLine()), false);
		System.out.println("Please enter a file name and location for the compressed NBT data");
		data.toFile(new File(sysIn.readLine()), true);
		System.out.println("Open these new files with another NBT library to check they're ok!");
	}
}