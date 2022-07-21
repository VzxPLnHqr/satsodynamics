# satsodynamics
Can we make quasi-classical thermodyanmic observations of the bitcoin network by
measuring the inflow/outflow of Heat (Q) and Work (W)? Can we do it from first
principles? Is the information gleaned useful at all? This project aims to answer
these questions!

Your thoughtful contributions are most welcome!

## What?
`Change in Internal Energy of an Isolated System (deltaU) = Work In (W) - Heat Out (Q)`.
Here we adopt the sign convention that `W` is positive when work is done _on_ the
system, and `Q` is positive when heat is released _by_ the system. This is not
necessarily the traditional sign convention, but it may make more sense for our
purposes.

Now, let us translate this over to the realm of bitcoin. We need a way to measure
work and heat in a manner which is compatible with the thermodynamic notion of
energy. We can accomplish this by re-interpreting the proof-of-work (PoW) information
which is included in valid bitcoin blocks. There are multiple such interpretations,
but here is one that we will start with:

## Proof of Work is Global and Cumulative
The idea illustrated briefly in [1] is similar to what we are envisioning here.
Specifically, when a new block is mined, we want the effect to be such that the
security provided by the PoW is applied to all existing UTXOs instantly and in 
a fashion such that older UTXOs effectively receive "more" than younger UTXOs 
(this is what ensures that it is more difficult for miners to rewrite an older 
transaction than a younger transaction. 

Implementing what is contemplated in [1] pre-supposes the notion of "blocks" and 
uses the "age" of a utxo in blocks (in blocks) as a means for tracking the accumulated PoW
to a UTXO. Here, on the other hand, we are trying to take a different approach
and keep the thermodynamic accounting at the per-transaction level as much as
possible. If succuessful, the thought is that the approach(es) here might be more
scalable and implementable since they rely less on global state.

### 1. A Naive Implementation

#### Mining = Work In, Spending = Heat Out.

1. When a valid bitcoin block, with work requirement `W` is mined, we treat this 
   as if there has been an amount of work `W` performed _on_ the system.
2. We consider the work `W` as spread evenly over the transaction outputs of the block,
   weighted according to the number of satoshis in the output relative to the total
   number of satoshis across all outputs in that block.
3. These outputs, along with the corresponding amount of work for each is included
   in the UTXO set. The `W` has crossed over into the system now and is part of
   the internal energy `U` of the system. In other words, each UTXO has associated
   with it not just the usual properties (number of satoshis, output script, ...),
   but also an amount of internal energy `u`.
4. When a UTXO with internal energy `u` is spent, we consider the system as releasing
   an amount `u` of heat.

##### What about empty blocks with no outputs?
This naive approach allocates work directly to the outputs for transactions within
a block. However, in the event that a block has no outputs whatsoever, then there
is no way to actually allocate the work, and it is released as heat. This is
unsatisfactory, since this completely discounts any additional security that an
empty block confers on the state of the system (the UTXO set).

#### Transaction fees as accumulated internal energy
What about transaction fees? There are a number of ways we can handle transaction
fees. Here we opt to treat transaction fees as accumulated/retained work by the
system. In other words, the amount of energy associated with fees is *not* released
as heat (at least not right now), but is instead accumulated as internal energy
of the system. This particular treatment may not be justified, and it may be more
realistic to simply also release the fees as heat. As stated above, there are a
number of different ways to perform thermodynamic analysis of this system.

##### Alternative treatments of fees
* Ignore them and just release the full amount of heat associated with them.
* Transfer the internal energy associated with fees over to the coinbase outputs.

## Status
* Pre-proof-of-concept (aka probably broken).

## Building / Usage (application)
The application currently being built in this repository is very simple:
1. use the [Nix](https://nixos.org) package manager to install dependencies by first installing Nix and then running `nix-shell -p scala` which will ensure that you have a decent version of scala/java installed
2. for the build, we use the `mill` build tool [Mill Website](https://com-lihaoyi.github.io/mill), which also requires java
3. a bootstrap script for mill has been checked into the repository already
4. `./mill -i main.run` runs the `main` module of the `bulid.sc` project (the `-i` allows for `readLine` and `ctrl+C` to work properly)
5. Reminder: open a local port (`ssh -L 8332:localhost:8332 my-cool-user@my-cool-website.com`) so that the app can communicate with your bitcoin full node, and make sure the values in `application.conf` are correct.

## Acknowledgments
This project takes inspiration from, and is thankful to:
....

## References
### Bitcoin-specific
[1] PoW as "Gravity" - https://medium.com/@laurentmt/gravity-10e1a25d2ab2

### General (Thermodynamics / Information-theory / Mathematics)
[2] E.T. Jaynes - Information Theory and Statistical Mechanics, 1957 - One of Jaynes earliest papers. The framework of statistical mechanics is shown to be useful for statistical inference in general, possibly even for non-physical systems/processes.
[3] John Baez, Mike Stay - Algorithmic thermodynamics, circa 2010-2013, [pdf](http://math.ucr.edu/home/baez/thermo.pdf), [blog post](http://golem.ph.utexas.edu/category/2010/02/algorithmic_thermodynamics.html)
    The model described here is "interesting" but not very similar to what we are contemplating in this project. Nevertheless, the methodology around how it might look to formalize something like this, along with the list of references, is quite helpful. 
