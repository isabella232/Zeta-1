import { expect } from 'chai';
import { shallowMount } from '@vue/test-utils';

describe('Zeta Ace',()=>{
  it('ace check format reg',()=>{
    const replace = (str: string)=>{
      const keyWords = 'ab';
      const reg = new RegExp('(' + keyWords + ')', 'gi');
      return str.replace(reg, ('<span class="highlight">$1</span>'));
    };
    const str = 'abca 111 abcasa a';
    expect(replace(str)).to.be.eq('<span class="highlight">ab</span>ca 111 <span class="highlight">ab</span>casa a');
  });
});